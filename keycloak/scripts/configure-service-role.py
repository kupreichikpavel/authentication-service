import base64
import json
import os
import urllib.error
import urllib.parse
import urllib.request

BASE_URL = os.getenv("KEYCLOAK_SERVER_URL", "http://localhost:8082")
REALM = os.getenv("KEYCLOAK_REALM", "shopper")

ADMIN_USERNAME = os.getenv("KEYCLOAK_ADMIN_USERNAME", "admin")
ADMIN_PASSWORD = os.environ["KEYCLOAK_ADMIN_PASSWORD"]

CLIENT_ID = os.getenv("KEYCLOAK_CLIENT_ID", "authentication-service")
CLIENT_SECRET = os.environ["AUTH_SERVICE_CLIENT_SECRET"]
ROLE_NAME = os.getenv("KEYCLOAK_SERVICE_ROLE", "SERVICE")


def request(
        url,
        method="GET",
        data=None,
        headers=None,
        allowed_statuses=(200, 204)
):
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers=headers or {}
    )

    try:
        with urllib.request.urlopen(req) as response:
            status = response.status
            body = response.read()

            if status not in allowed_statuses:
                raise SystemExit(
                    f"{method} {url} -> HTTP {status}"
                )

            if not body:
                return None

            return json.loads(body)
    except urllib.error.HTTPError as exception:
        body = exception.read().decode(errors="replace")

        raise SystemExit(
            f"{method} {url} -> HTTP {exception.code}\n{body}"
        )


admin_form = urllib.parse.urlencode({
    "grant_type": "password",
    "client_id": "admin-cli",
    "username": ADMIN_USERNAME,
    "password": ADMIN_PASSWORD,
}).encode()

admin_token_response = request(
    f"{BASE_URL}/realms/master/protocol/openid-connect/token",
    method="POST",
    data=admin_form,
    headers={
        "Content-Type": "application/x-www-form-urlencoded"
    }
)

admin_headers = {
    "Authorization":
        f"Bearer {admin_token_response['access_token']}",
    "Content-Type": "application/json",
}

clients = request(
    f"{BASE_URL}/admin/realms/{REALM}/clients"
    f"?clientId={urllib.parse.quote(CLIENT_ID)}",
    headers=admin_headers
)

client = next(
    (
        item
        for item in clients
        if item.get("clientId") == CLIENT_ID
    ),
    None
)

if client is None:
    raise SystemExit(
        f"Client {CLIENT_ID!r} не найден"
    )

client_uuid = client["id"]

service_account = request(
    f"{BASE_URL}/admin/realms/{REALM}/clients/"
    f"{client_uuid}/service-account-user",
    headers=admin_headers
)

service_account_id = service_account["id"]

roles = request(
    f"{BASE_URL}/admin/realms/{REALM}/roles",
    headers=admin_headers
)

role = next(
    (
        item
        for item in roles
        if item.get("name") == ROLE_NAME
    ),
    None
)

if role is None:
    request(
        f"{BASE_URL}/admin/realms/{REALM}/roles",
        method="POST",
        data=json.dumps({
            "name": ROLE_NAME,
            "description":
                "Internal authentication-service access"
        }).encode(),
        headers=admin_headers,
        allowed_statuses=(201, 204)
    )

    role = request(
        f"{BASE_URL}/admin/realms/{REALM}/roles/{ROLE_NAME}",
        headers=admin_headers
    )

    print(f"Created realm role: {ROLE_NAME}")
else:
    print(f"Realm role already exists: {ROLE_NAME}")

assigned_roles = request(
    f"{BASE_URL}/admin/realms/{REALM}/users/"
    f"{service_account_id}/role-mappings/realm",
    headers=admin_headers
)

already_assigned = any(
    item.get("name") == ROLE_NAME
    for item in assigned_roles
)

if not already_assigned:
    request(
        f"{BASE_URL}/admin/realms/{REALM}/users/"
        f"{service_account_id}/role-mappings/realm",
        method="POST",
        data=json.dumps([role]).encode(),
        headers=admin_headers
    )

    print(
        f"Assigned {ROLE_NAME} to service account "
        f"of {CLIENT_ID}"
    )
else:
    print(
        f"{ROLE_NAME} is already assigned "
        f"to service account of {CLIENT_ID}"
    )

service_form = urllib.parse.urlencode({
    "grant_type": "client_credentials",
    "client_id": CLIENT_ID,
    "client_secret": CLIENT_SECRET,
}).encode()

service_token_response = request(
    f"{BASE_URL}/realms/{REALM}/protocol/openid-connect/token",
    method="POST",
    data=service_form,
    headers={
        "Content-Type": "application/x-www-form-urlencoded"
    }
)

token = service_token_response["access_token"]
payload = token.split(".")[1]
payload += "=" * (-len(payload) % 4)

claims = json.loads(
    base64.urlsafe_b64decode(payload)
)

audience = claims.get("aud", [])
roles = claims.get(
    "realm_access",
    {}
).get("roles", [])

if isinstance(audience, str):
    audience = [audience]

print()
print("Service token claims:")
print(json.dumps({
    "azp": claims.get("azp"),
    "aud": audience,
    "roles": roles,
}, indent=2, ensure_ascii=False))

if "user-service" not in audience:
    raise SystemExit(
        "ERROR: service token audience "
        "не содержит user-service"
    )

if ROLE_NAME not in roles:
    raise SystemExit(
        f"ERROR: service token roles "
        f"не содержит {ROLE_NAME}"
    )

print()
print("OK: service token готов для user-service")
