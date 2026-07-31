import json
import os
import urllib.error
import urllib.parse
import urllib.request

BASE_URL = os.getenv(
    "KEYCLOAK_SERVER_URL",
    "http://localhost:8082"
)
REALM = os.getenv("KEYCLOAK_REALM", "shopper")

ADMIN_USERNAME = os.getenv(
    "KEYCLOAK_ADMIN_USERNAME",
    "admin"
)
ADMIN_PASSWORD = os.environ["KEYCLOAK_ADMIN_PASSWORD"]


def request(
        url: str,
        method: str = "GET",
        data: bytes | None = None,
        headers: dict[str, str] | None = None
):
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers=headers or {}
    )

    try:
        with urllib.request.urlopen(req) as response:
            body = response.read()

            if not body:
                return None

            return json.loads(body)
    except urllib.error.HTTPError as exception:
        body = exception.read().decode(errors="replace")

        raise SystemExit(
            f"{method} {url} -> HTTP {exception.code}\n"
            f"{body}"
        )


token_data = urllib.parse.urlencode({
    "grant_type": "password",
    "client_id": "admin-cli",
    "username": ADMIN_USERNAME,
    "password": ADMIN_PASSWORD,
}).encode()

token_response = request(
    f"{BASE_URL}/realms/master/"
    "protocol/openid-connect/token",
    method="POST",
    data=token_data,
    headers={
        "Content-Type":
            "application/x-www-form-urlencoded"
    }
)

access_token = token_response["access_token"]

headers = {
    "Authorization": f"Bearer {access_token}",
    "Content-Type": "application/json",
}

profile_url = (
    f"{BASE_URL}/admin/realms/"
    f"{REALM}/users/profile"
)

profile = request(
    profile_url,
    headers=headers
)

attributes = profile.setdefault("attributes", [])

user_id_attribute = {
    "name": "userId",
    "displayName": "User ID",
    "multivalued": False,
    "permissions": {
        "view": [
            "admin",
            "user"
        ],
        "edit": [
            "admin"
        ]
    },
    "validations": {
        "length": {
            "max": 20
        }
    }
}

existing_attribute = next(
    (
        attribute
        for attribute in attributes
        if attribute.get("name") == "userId"
    ),
    None
)

if existing_attribute is None:
    attributes.append(user_id_attribute)
    print("Adding managed attribute: userId")
else:
    existing_attribute.update(user_id_attribute)
    print("Updating managed attribute: userId")

request(
    profile_url,
    method="PUT",
    data=json.dumps(profile).encode(),
    headers=headers
)

updated_profile = request(
    profile_url,
    headers=headers
)

updated_attribute = next(
    (
        attribute
        for attribute
        in updated_profile.get("attributes", [])
        if attribute.get("name") == "userId"
    ),
    None
)

if updated_attribute is None:
    raise SystemExit(
        "ERROR: userId was not added to User Profile"
    )

print("OK: userId is a managed Keycloak attribute")
print(json.dumps(
    updated_attribute,
    indent=2,
    ensure_ascii=False
))
