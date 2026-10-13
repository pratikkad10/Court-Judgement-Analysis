import httpx
import pytest
import respx
from judgment_common.llm_router.base import BaseProvider, ProviderError, ProviderResult
from judgment_common.llm_router.providers import OpenAiCompatibleProvider
from judgment_common.llm_router.router import AllProvidersFailedError, LlmRouter

MESSAGES = [{"role": "user", "content": "hello"}]

def provider(name: str) -> OpenAiCompatibleProvider:
    return OpenAiCompatibleProvider(name, "key", "model-from-env", f"https://{name}.example/v1/chat", 0.01)

def good_response(text: str = "ok") -> httpx.Response:
    return httpx.Response(200, json={"choices": [{"message": {"content": text}}]})

@pytest.mark.asyncio
@respx.mock
async def test_falls_back_after_429():
    first, second = provider("first"), provider("second")
    respx.post(first.url).mock(return_value=httpx.Response(429, headers={"Retry-After": "2"}))
    respx.post(second.url).mock(return_value=good_response())
    result = await LlmRouter([first, second]).generate(MESSAGES)
    assert result.provider == "second"

@pytest.mark.asyncio
@respx.mock
async def test_falls_back_after_5xx():
    first, second = provider("first"), provider("second")
    respx.post(first.url).mock(return_value=httpx.Response(503))
    respx.post(second.url).mock(return_value=good_response())
    assert (await LlmRouter([first, second]).generate(MESSAGES)).provider == "second"

@pytest.mark.asyncio
@respx.mock
async def test_falls_back_after_timeout():
    first, second = provider("first"), provider("second")
    respx.post(first.url).mock(side_effect=httpx.ReadTimeout("late"))
    respx.post(second.url).mock(return_value=good_response())
    assert (await LlmRouter([first, second]).generate(MESSAGES)).provider == "second"

@pytest.mark.asyncio
@respx.mock
async def test_invalid_json_in_json_mode_falls_back():
    first, second = provider("first"), provider("second")
    respx.post(first.url).mock(return_value=good_response("not json"))
    respx.post(second.url).mock(return_value=good_response('{"value":"ok"}'))
    assert (await LlmRouter([first, second]).generate(MESSAGES, json_mode=True)).provider == "second"

class AlwaysFails(BaseProvider):
    name = "failing"
    model = "model-from-env"
    configured = True
    async def generate(self, *args, **kwargs):
        raise ProviderError("down", 503)

class Works(BaseProvider):
    name = "working"
    model = "model-from-env"
    configured = True
    def __init__(self): self.calls = 0
    async def generate(self, *args, **kwargs):
        self.calls += 1
        return ProviderResult("ok", self.name, self.model, 1, {})

@pytest.mark.asyncio
async def test_circuit_breaker_skips_then_recovers_after_cooldown():
    now = [0.0]
    bad, good = AlwaysFails(), Works()
    router = LlmRouter([bad, good], cooldown_seconds=10, clock=lambda: now[0])
    for _ in range(3):
        await router.generate(MESSAGES)
    assert router.circuits["failing"].blocked_until == 10
    await router.generate(MESSAGES)
    assert good.calls == 4
    now[0] = 11
    await router.generate(MESSAGES)
    assert router.circuits["failing"].failures == 4

@pytest.mark.asyncio
async def test_raises_when_every_provider_fails():
    with pytest.raises(AllProvidersFailedError, match="No configured"):
        await LlmRouter([AlwaysFails()]).generate(MESSAGES)
