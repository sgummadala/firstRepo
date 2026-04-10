import os
from fastapi.staticfiles import StaticFiles
from google.adk.cli.fast_api import get_fast_api_app

app = get_fast_api_app(
    agents_dir=os.path.dirname(os.path.abspath(__file__)),
    allow_origins=["*"] ,
    web=False
)

app.mount("/ui", StaticFiles(directory="frontend", html=True), name="frontend")

