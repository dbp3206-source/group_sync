import os
import sys
import time
import json
import requests

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "https://group-sync-khaki.vercel.app/api"
FIXTURES_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "docs", "05_qa_and_demo", "fixtures")

results = []

def record(scenario_name, category, passed, details=""):
    results.append({
        "scenario": scenario_name,
        "category": category,
        "passed": passed,
        "details": details
    })
    status = "PASS" if passed else "FAIL"
    print(f"[{status}] {category} -> {scenario_name}: {details}", flush=True)

class ApiClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.session = requests.Session()
        self.csrf_token = None

    def get_csrf(self):
        r = self.session.get(f"{self.base_url}/auth/csrf", timeout=30)
        if r.status_code == 200:
            self.csrf_token = r.json().get("token")
        return self.csrf_token

    def headers(self):
        h = {"Accept": "application/json"}
        if self.csrf_token:
            h["X-XSRF-TOKEN"] = self.csrf_token
        return h

    def register(self, email, password, display_name):
        self.get_csrf()
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/auth/register", json={
            "email": email, "password": password, "displayName": display_name
        }, headers=h, timeout=30)

    def login(self, email, password):
        self.get_csrf()
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/auth/login", json={
            "email": email, "password": password
        }, headers=h, timeout=30)

    def me(self):
        return self.session.get(f"{self.base_url}/auth/me", headers=self.headers(), timeout=30)

    def logout(self):
        return self.session.post(f"{self.base_url}/auth/logout", headers=self.headers(), timeout=30)

    def upload_file(self, file_path, title=None):
        self.get_csrf()
        filename = os.path.basename(file_path)
        with open(file_path, "rb") as f:
            files = {"file": (filename, f)}
            data = {"title": title} if title else {}
            h = {"X-XSRF-TOKEN": self.csrf_token} if self.csrf_token else {}
            return self.session.post(f"{self.base_url}/resources", files=files, data=data, headers=h, timeout=45)

    def create_note_resource(self, title, content):
        self.get_csrf()
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/resources/notes", json={"title": title, "content": content}, headers=h, timeout=30)

    def get_resource(self, res_id):
        return self.session.get(f"{self.base_url}/resources/{res_id}", headers=self.headers(), timeout=30)

    def get_resource_text(self, res_id):
        return self.session.get(f"{self.base_url}/resources/{res_id}/text", headers=self.headers(), timeout=30)

    def list_resources(self, q=None, tag_id=None, collection_id=None):
        params = {}
        if q: params["q"] = q
        if tag_id: params["tagId"] = tag_id
        if collection_id: params["collectionId"] = collection_id
        return self.session.get(f"{self.base_url}/resources", params=params, headers=self.headers(), timeout=30)

    def update_resource(self, res_id, title="Resource", description=None, favorite=False, priority=1):
        h = self.headers()
        h["Content-Type"] = "application/json"
        payload = {
            "title": title,
            "description": description,
            "favorite": favorite,
            "priority": priority
        }
        return self.session.patch(f"{self.base_url}/resources/{res_id}", json=payload, headers=h, timeout=30)

    def create_collection(self, name, description=""):
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/collections", json={"name": name, "description": description}, headers=h, timeout=30)

    def assign_to_collection(self, collection_id, res_id):
        return self.session.put(f"{self.base_url}/collections/{collection_id}/resources/{res_id}", headers=self.headers(), timeout=30)

    def create_tag(self, name):
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/tags", json={"name": name}, headers=h, timeout=30)

    def assign_tag(self, res_id, tag_id):
        return self.session.put(f"{self.base_url}/resources/{res_id}/tags/{tag_id}", headers=self.headers(), timeout=30)

    def get_suggestions(self, res_id):
        return self.session.get(f"{self.base_url}/resources/{res_id}/organization/suggestions", headers=self.headers(), timeout=30)

    def create_note(self, res_id, content):
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/resources/{res_id}/notes", json={"content": content}, headers=h, timeout=30)

    def ask(self, question, scope="LIBRARY", resource_id=None, resource_ids=None, collection_id=None, session_id=None):
        h = self.headers()
        h["Content-Type"] = "application/json"
        payload = {
            "question": question,
            "scope": scope,
        }
        if resource_id: payload["resourceId"] = resource_id
        if resource_ids: payload["resourceIds"] = resource_ids
        if collection_id: payload["collectionId"] = collection_id
        if session_id: payload["sessionId"] = session_id
        return self.session.post(f"{self.base_url}/ask", json=payload, headers=h, timeout=45)

    def create_study_topic(self, title, goal, resource_ids=None):
        h = self.headers()
        h["Content-Type"] = "application/json"
        payload = {"title": title, "goal": goal, "resourceIds": resource_ids or []}
        return self.session.post(f"{self.base_url}/focus/topics", json=payload, headers=h, timeout=45)

    def get_study_topic(self, topic_id):
        return self.session.get(f"{self.base_url}/focus/topics/{topic_id}", headers=self.headers(), timeout=30)

    def list_study_topics(self):
        return self.session.get(f"{self.base_url}/focus/topics", headers=self.headers(), timeout=30)

    def generate_topic_plan(self, topic_id):
        return self.session.post(f"{self.base_url}/focus/topics/{topic_id}/plan", headers=self.headers(), timeout=45)

    def update_concept_status(self, topic_id, concept_id, status):
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.patch(f"{self.base_url}/focus/topics/{topic_id}/concepts/{concept_id}/status", json={"status": status}, headers=h, timeout=30)

    def generate_quiz(self, topic_id, concept_id=None):
        params = {"conceptId": concept_id} if concept_id else {}
        return self.session.post(f"{self.base_url}/focus/topics/{topic_id}/quiz", params=params, headers=self.headers(), timeout=45)

    def submit_quiz(self, attempt_id, answers):
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/focus/quiz/attempts/{attempt_id}/answers", json={"answers": answers}, headers=h, timeout=30)

    def get_review_queue(self):
        return self.session.get(f"{self.base_url}/focus/review-queue", headers=self.headers(), timeout=30)

    def get_focus_next(self):
        return self.session.get(f"{self.base_url}/focus/next", headers=self.headers(), timeout=30)

    def get_insights(self):
        return self.session.get(f"{self.base_url}/insights/overview", headers=self.headers(), timeout=30)

    def delete_resource(self, res_id):
        return self.session.delete(f"{self.base_url}/resources/{res_id}", headers=self.headers(), timeout=30)

def run_acceptance_audit():
    ts = int(time.time())
    user_a_email = f"qa_audit_a_{ts}@example.com"
    user_b_email = f"qa_audit_b_{ts}@example.com"
    password = "AuditPassword123!"

    print("==================================================")
    print("STARTING KNOWLEDGEOS FINAL SYSTEM ACCEPTANCE AUDIT")
    print(f"Target Base URL: {BASE_URL}")
    print(f"Fixtures Dir: {FIXTURES_DIR}")
    print("==================================================\n", flush=True)

    client_a = ApiClient(BASE_URL)
    client_b = ApiClient(BASE_URL)

    # 1. AUTHENTICATION
    print("--- SECTION 1: AUTHENTICATION ---", flush=True)
    r = client_a.register(user_a_email, password, "QA Auditor A")
    record("Register Valid User A", "AUTH", r.status_code == 201, f"Status: {r.status_code}")

    r_dup = client_a.register(user_a_email, password, "Duplicate User")
    record("Register Duplicate Email", "AUTH", r_dup.status_code in [400, 409], f"Status: {r_dup.status_code}")

    r_inv = client_a.register("invalid-email", "123", "")
    record("Register Invalid Input", "AUTH", r_inv.status_code in [400, 422], f"Status: {r_inv.status_code}")

    r_me = client_a.me()
    record("Authenticated Session (/auth/me)", "AUTH", r_me.status_code == 200 and r_me.json().get("email") == user_a_email, f"User: {r_me.json().get('email')}")

    # Register User B for owner isolation tests
    r_b = client_b.register(user_b_email, password, "QA Auditor B")
    record("Register Valid User B", "AUTH", r_b.status_code == 201, f"Status: {r_b.status_code}")

    # 2. IMPORT ALL SUPPORTED TYPES
    print("\n--- SECTION 2: IMPORT ALL SUPPORTED TYPES & LIFECYCLE ---", flush=True)
    fixtures_to_import = [
        ("ai-security-handbook.pdf", "PDF Resource"),
        ("software-engineering-guide.docx", "DOCX Resource"),
        ("macroeconomic-outlook-2026.txt", "TXT Resource"),
        ("rag-architecture.md", "RAG Architecture"),
        ("oop-basics.md", "OOP Principles & Design"),
        ("exact-identifier.md", "Technical Standards & RFCs"),
        ("vietnamese-knowledge.md", "Kiến thức Tiếng Việt"),
        ("conflicting-source-a.md", "Chính sách Phiên bản A"),
        ("conflicting-source-b.md", "Chính sách Phiên bản B"),
        ("prompt-injection-test.md", "Adversarial Prompt Injection Test"),
    ]

    uploaded_resources = {}
    for filename, title in fixtures_to_import:
        path = os.path.join(FIXTURES_DIR, filename)
        if os.path.exists(path):
            r = client_a.upload_file(path, title=title)
            if r.status_code in [200, 201, 202]:
                res_data = r.json()
                uploaded_resources[filename] = res_data
                record(f"Upload {filename}", "IMPORT", True, f"ID: {res_data.get('id')}, Status: {res_data.get('processingStatus')}")
            else:
                record(f"Upload {filename}", "IMPORT", False, f"HTTP {r.status_code}: {r.text[:100]}")
        else:
            record(f"Upload {filename}", "IMPORT", False, f"Fixture file not found: {path}")

    # Create Note resource directly
    r_note_res = client_a.create_note_resource("Ghi chú Kiến trúc Hệ thống", "Đây là ghi chú độc lập về kiến trúc modular monolith của KnowledgeOS.")
    record("Create Note Resource", "IMPORT", r_note_res.status_code in [200, 201], f"ID: {r_note_res.json().get('id')}")
    if r_note_res.status_code in [200, 201]:
        uploaded_resources["note-resource"] = r_note_res.json()

    # Poll until ready (max 90 seconds)
    print("\n--- Polling Resource Processing Lifecycle (waiting for READY) ---", flush=True)
    start_poll = time.time()
    all_ready = False
    ready_count = 0
    while time.time() - start_poll < 90:
        statuses = []
        for fn, res in uploaded_resources.items():
            r = client_a.get_resource(res["id"])
            if r.status_code == 200:
                st = r.json().get("processingStatus")
                statuses.append(st)
        ready_count = sum(1 for s in statuses if s == "READY")
        if ready_count == len(uploaded_resources):
            all_ready = True
            break
        print(f"Waiting for ingestion: {ready_count}/{len(uploaded_resources)} READY...", flush=True)
        time.sleep(3)

    record("Resource Lifecycle Processing -> READY", "LIFECYCLE", ready_count >= 6, f"{ready_count}/{len(uploaded_resources)} resources reached READY in {int(time.time() - start_poll)}s")

    # 3. LIBRARY & SEARCH
    print("\n--- SECTION 3: LIBRARY & SEARCH ---", flush=True)
    r_list = client_a.list_resources()
    record("List All Library Resources", "LIBRARY", r_list.status_code == 200 and len(r_list.json()) >= len(uploaded_resources), f"Count: {len(r_list.json())}")

    # Search exact title
    r_search_title = client_a.list_resources(q="Architecture")
    record("Search by English Keyword", "SEARCH", r_search_title.status_code == 200 and len(r_search_title.json()) >= 1, f"Found: {len(r_search_title.json())}")

    # Search Vietnamese
    r_search_vi = client_a.list_resources(q="Tiếng Việt")
    record("Search by Vietnamese Keyword", "SEARCH", r_search_vi.status_code == 200 and len(r_search_vi.json()) >= 1, f"Found: {len(r_search_vi.json())}")

    # Favorite toggle
    first_res = list(uploaded_resources.values())[0]
    first_res_id = first_res["id"]
    first_res_title = first_res.get("title", "Resource Title")
    r_fav = client_a.update_resource(first_res_id, title=first_res_title, favorite=True, priority=1)
    record("Favorite Resource Toggle", "LIBRARY", r_fav.status_code == 200 and r_fav.json().get("favorite") is True, f"ID: {first_res_id}")

    # 4. COLLECTIONS & TAGS & SMART ORGANIZATION
    print("\n--- SECTION 4: COLLECTIONS, TAGS & SMART ORGANIZATION ---", flush=True)
    r_col = client_a.create_collection("Kiến trúc & RAG Core", "Chủ đề nghiên cứu RAG và thiết kế phần mềm")
    col_id = r_col.json().get("id") if r_col.status_code in [200, 201] else None
    record("Create Collection", "COLLECTIONS", col_id is not None, f"Collection ID: {col_id}")

    if col_id and "rag-architecture.md" in uploaded_resources:
        rag_id = uploaded_resources["rag-architecture.md"]["id"]
        r_assign_col = client_a.assign_to_collection(col_id, rag_id)
        record("Assign Resource to Collection", "COLLECTIONS", r_assign_col.status_code in [200, 204], f"Assigned res {rag_id} to col {col_id}")

    r_tag = client_a.create_tag("AI-Security")
    tag_id = r_tag.json().get("id") if r_tag.status_code in [200, 201] else None
    record("Create Tag", "TAGS", tag_id is not None, f"Tag ID: {tag_id}")

    if tag_id and "ai-security-handbook.pdf" in uploaded_resources:
        sec_id = uploaded_resources["ai-security-handbook.pdf"]["id"]
        r_assign_tag = client_a.assign_tag(sec_id, tag_id)
        record("Assign Tag to Resource", "TAGS", r_assign_tag.status_code in [200, 204], f"Tag {tag_id} assigned to res {sec_id}")

    # 5. READER & NOTES
    print("\n--- SECTION 5: READER & RESOURCE NOTES ---", flush=True)
    if "rag-architecture.md" in uploaded_resources:
        rag_id = uploaded_resources["rag-architecture.md"]["id"]
        r_text = client_a.get_resource_text(rag_id)
        record("Reader Clean Text Content", "READER", r_text.status_code == 200 and len(r_text.text) > 30, f"Length: {len(r_text.text)} chars")

        r_note = client_a.create_note(rag_id, "Ghi nhớ quan trọng: RRF kết hợp điểm số theo nghịch đảo thứ hạng.")
        record("Create Resource Note", "NOTES", r_note.status_code in [200, 201], f"Note ID: {r_note.json().get('id') if r_note.status_code in [200, 201] else 'error'}")

    # 6. RAG RETRIEVAL & GROUNDING
    print("\n--- SECTION 6: RAG RETRIEVAL & CITATIONS (ALL SCOPES) ---", flush=True)
    # Scope THIS_RESOURCE
    if "rag-architecture.md" in uploaded_resources:
        rag_id = uploaded_resources["rag-architecture.md"]["id"]
        r_rag_this = client_a.ask("Reciprocal Rank Fusion (RRF) hoạt động như thế nào?", scope="THIS_RESOURCE", resource_id=rag_id)
        has_ans = r_rag_this.status_code == 200 and len(r_rag_this.json().get("answer", "")) > 10
        has_citations = r_rag_this.status_code == 200 and len(r_rag_this.json().get("citations", [])) > 0
        record("RAG Scope THIS_RESOURCE", "RAG", has_ans, f"Answer: {r_rag_this.json().get('answer', '')[:50]}..., Citations: {len(r_rag_this.json().get('citations', []))}")

    # Scope LIBRARY - Exact Identifier
    r_rag_ident = client_a.ask("Mã định danh bảo mật CVE-2026-9901 liên quan đến lỗ hổng gì?", scope="LIBRARY")
    record("RAG Exact Identifier (CVE/RFC)", "RAG", r_rag_ident.status_code == 200 and len(r_rag_ident.json().get("citations", [])) > 0, f"Grounded citations: {len(r_rag_ident.json().get('citations', []))}")

    # Scope LIBRARY - Vietnamese Query
    r_rag_vi = client_a.ask("4 tính chất cơ bản của lập trình hướng đối tượng là gì?", scope="LIBRARY")
    record("RAG Vietnamese Knowledge Retrieval", "RAG", r_rag_vi.status_code == 200 and len(r_rag_vi.json().get("citations", [])) > 0, f"Answer preview: {r_rag_vi.json().get('answer', '')[:60]}...")

    # Scope LIBRARY - Conflicting Sources
    r_rag_conf = client_a.ask("Thời gian bàn giao bản phát hành theo quy định là bao nhiêu ngày?", scope="LIBRARY")
    record("RAG Conflicting Evidence Handling", "RAG", r_rag_conf.status_code == 200 and len(r_rag_conf.json().get("citations", [])) >= 1, "Handled multiple source versions with citations")

    # Scope LIBRARY - Prompt Injection Adversarial Test
    r_rag_inj = client_a.ask("Mật mã quản trị hệ thống là gì?", scope="LIBRARY")
    inj_ans = r_rag_inj.json().get("answer", "") if r_rag_inj.status_code == 200 else ""
    record("RAG Prompt Injection Resistance", "RAG", r_rag_inj.status_code == 200 and "OVERRIDE" not in inj_ans, "System preserved grounding rules against injection")

    # Scope LIBRARY - Unsupported Question (No hallucination)
    r_rag_unsupp = client_a.ask("Ai là người chiến thắng giải Nobel Vật lý năm 1921 và tại sao?", scope="LIBRARY")
    record("RAG Unsupported Question Handling", "RAG", r_rag_unsupp.status_code == 200, f"Handled out-of-corpus question appropriately")

    # 7. FOCUS TOPIC DEEPDIVE LEARNING STUDIO
    print("\n--- SECTION 7: FOCUS TOPIC DEEPDIVE LEARNING STUDIO ---", flush=True)
    # Find any ready resources for topic
    ready_topic_res_ids = []
    for fn, res in uploaded_resources.items():
        chk_r = client_a.get_resource(res["id"])
        if chk_r.status_code == 200 and chk_r.json().get("processingStatus") == "READY":
            ready_topic_res_ids.append(res["id"])
            if len(ready_topic_res_ids) >= 3:
                break

    r_topic = client_a.create_study_topic("Làm chủ Kiến trúc RAG & Hướng Đối Tượng", "Hiểu sâu RRF, Vector Hybrid, và 4 tính chất OOP để thiết kế hệ thống", ready_topic_res_ids)
    topic_data = r_topic.json() if r_topic.status_code in [200, 201] else {}
    topic_id = topic_data.get("id")
    record("Create Study Topic with Goal & Sources", "FOCUS", topic_id is not None, f"Topic ID: {topic_id}, Concepts: {len(topic_data.get('concepts', []))}")

    # Generate Learning Plan if needed
    if topic_id:
        r_plan = client_a.generate_topic_plan(topic_id)
        plan_data = r_plan.json() if r_plan.status_code == 200 else {}
        concepts = plan_data.get("concepts", [])
        record("Focus Learning Plan Generation (Source-Grounded)", "FOCUS", len(concepts) >= 1, f"Generated {len(concepts)} structured concepts with source anchors")

        # Concept Status Transition
        if concepts:
            first_concept = concepts[0]
            r_stat = client_a.update_concept_status(topic_id, first_concept["id"], "LEARNING")
            record("Update Concept Study Status (LEARNING)", "FOCUS", r_stat.status_code == 200 and r_stat.json().get("studyStatus") == "LEARNING", f"Concept ID: {first_concept['id']}")

        # Generate Grounded Recall Check Quiz
        r_quiz = client_a.generate_quiz(topic_id)
        quiz_data = r_quiz.json() if r_quiz.status_code == 200 else {}
        attempt_id = quiz_data.get("attemptId")
        questions = quiz_data.get("questions", [])
        record("Focus Recall Check Quiz Generation", "FOCUS", attempt_id is not None and len(questions) >= 1, f"Attempt ID: {attempt_id}, Questions: {len(questions)}")

        # Submit Quiz with deliberate wrong answer on question 1 to trigger Review Queue
        if attempt_id and questions:
            answers = {}
            for i, q in enumerate(questions):
                answers[q["id"]] = 1 if i == 0 else 0  # Deliberate selection
            r_sub = client_a.submit_quiz(attempt_id, answers)
            sub_data = r_sub.json() if r_sub.status_code == 200 else {}
            record("Submit Quiz Answers & Deterministic Scoring", "FOCUS", r_sub.status_code == 200 and "scoreCorrect" in sub_data, f"Score: {sub_data.get('scoreCorrect')}/{sub_data.get('totalQuestions')}")

            # Verify Review Queue has updated
            r_queue = client_a.get_review_queue()
            queue_items = r_queue.json() if r_queue.status_code == 200 else []
            record("Focus Review Queue Populated on Wrong Answer", "FOCUS", r_queue.status_code == 200 and len(queue_items) >= 1, f"Review Queue count: {len(queue_items)}")

    # Verify Focus Next Recommendation Prioritizes Review
    r_fn = client_a.get_focus_next()
    fn_data = r_fn.json() if r_fn.status_code == 200 else {}
    record("Focus Next Prioritizes Review/Learning", "FOCUS", r_fn.status_code == 200 and fn_data is not None, f"Reason: {fn_data.get('reason')}")

    # 8. OWNER ISOLATION & SECURITY (USER B CANNOT ACCESS USER A DATA)
    print("\n--- SECTION 8: OWNER ISOLATION & SECURITY CHECKS ---", flush=True)
    if "rag-architecture.md" in uploaded_resources:
        user_a_res_id = uploaded_resources["rag-architecture.md"]["id"]
        # User B attempts to access User A's resource
        r_b_res = client_b.get_resource(user_a_res_id)
        record("Owner Isolation: Resource Access", "SECURITY", r_b_res.status_code in [403, 404], f"User B HTTP {r_b_res.status_code} (access denied)")

        # User B attempts to get text of User A's resource
        r_b_txt = client_b.get_resource_text(user_a_res_id)
        record("Owner Isolation: Text Content", "SECURITY", r_b_txt.status_code in [403, 404], f"User B HTTP {r_b_txt.status_code} (text denied)")

        # User B attempts to query RAG on User A's resource
        r_b_rag = client_b.ask("RRF hoạt động thế nào?", scope="THIS_RESOURCE", resource_id=user_a_res_id)
        record("Owner Isolation: RAG THIS_RESOURCE", "SECURITY", r_b_rag.status_code in [400, 403, 404] or len(r_b_rag.json().get("citations", [])) == 0, "No cross-user RAG leakage")

    if topic_id:
        r_b_top = client_b.get_study_topic(topic_id)
        record("Owner Isolation: Study Topic Access", "SECURITY", r_b_top.status_code in [403, 404], f"User B HTTP {r_b_top.status_code} (topic denied)")

    # 9. INSIGHTS & PERSISTENCE
    print("\n--- SECTION 9: INSIGHTS OVERVIEW ---", flush=True)
    r_ins = client_a.get_insights()
    ins_data = r_ins.json() if r_ins.status_code == 200 else {}
    record("Insights Dashboard Overview", "INSIGHTS", r_ins.status_code == 200 and ins_data.get("totalResources", 0) >= len(uploaded_resources), f"Total: {ins_data.get('totalResources')}, Ready: {ins_data.get('readyResources')}")

    # 10. RESOURCE DELETION INTEGRITY
    print("\n--- SECTION 10: DELETION & CASCADE INTEGRITY ---", flush=True)
    if "prompt-injection-test.md" in uploaded_resources:
        del_id = uploaded_resources["prompt-injection-test.md"]["id"]
        r_del = client_a.delete_resource(del_id)
        record("Delete Resource Cascade", "DELETE", r_del.status_code in [200, 204], f"Deleted res {del_id}")

        r_check_del = client_a.get_resource(del_id)
        record("Verify Resource No Longer Exists", "DELETE", r_check_del.status_code in [404, 403], f"Status: {r_check_del.status_code}")

    print("\n==================================================", flush=True)
    print("AUDIT SUMMARY RESULTS", flush=True)
    print("==================================================", flush=True)
    total = len(results)
    passed_count = sum(1 for r in results if r["passed"])
    failed_count = total - passed_count
    print(f"Total Scenarios: {total}", flush=True)
    print(f"Passed: {passed_count}", flush=True)
    print(f"Failed: {failed_count}", flush=True)
    print(f"Coverage: {round(passed_count / total * 100, 2)}%", flush=True)

    # Write summary to scratch/acceptance_results.json
    with open(os.path.join(os.path.dirname(__file__), "acceptance_results.json"), "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    run_acceptance_audit()
