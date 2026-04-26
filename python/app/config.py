from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    web_api_url: str = "http://localhost:3000"
    internal_secret: str = "change-me"
    anthropic_api_key: str | None = None
    blob_read_write_token: str | None = None
    port: int = 8000


settings = Settings()
