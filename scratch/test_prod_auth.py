import requests
import json

base_url = "https://group-sync-khaki.vercel.app/api"
session = requests.Session()

print("1. Fetching CSRF token...")
r_csrf = session.get(f"{base_url}/auth/csrf", timeout=30)
print(f"CSRF Status: {r_csrf.status_code}, body: {r_csrf.text}")
csrf_token = r_csrf.json().get("token")
print(f"CSRF Token: {csrf_token}")
print(f"Cookies: {session.cookies.get_dict()}")

headers = {
    "Content-Type": "application/json",
    "X-XSRF-TOKEN": csrf_token
}

email = "qa_audit_live_01@example.com"
password = "Password123!"

print("\n2. Registering QA user...")
reg_payload = {
    "email": email,
    "password": password,
    "displayName": "QA Audit Live User"
}
r_reg = session.post(f"{base_url}/auth/register", json=reg_payload, headers=headers, timeout=30)
print(f"Register Status: {r_reg.status_code}, body: {r_reg.text}")

print("\n3. Testing /auth/me...")
r_me = session.get(f"{base_url}/auth/me", headers=headers, timeout=30)
print(f"Me Status: {r_me.status_code}, body: {r_me.text}")

print("\n4. Logging out...")
r_out = session.post(f"{base_url}/auth/logout", headers=headers, timeout=30)
print(f"Logout Status: {r_out.status_code}")

print("\n5. Logging back in with fresh session...")
session2 = requests.Session()
r_csrf2 = session2.get(f"{base_url}/auth/csrf", timeout=30)
csrf_token2 = r_csrf2.json().get("token")
headers2 = {
    "Content-Type": "application/json",
    "X-XSRF-TOKEN": csrf_token2
}
login_payload = {
    "email": email,
    "password": password
}
r_login = session2.post(f"{base_url}/auth/login", json=login_payload, headers=headers2, timeout=30)
print(f"Login Status: {r_login.status_code}, body: {r_login.text}")
print(f"Cookies after login: {session2.cookies.get_dict()}")

r_me2 = session2.get(f"{base_url}/auth/me", headers=headers2, timeout=30)
print(f"Me Status after login: {r_me2.status_code}, body: {r_me2.text}")
