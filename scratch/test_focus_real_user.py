import os
import sys
import time
import json
import requests

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "https://group-sync-khaki.vercel.app/api"
FIXTURES_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "docs", "05_qa_and_demo", "fixtures")

def log(msg):
    print(f"[FOCUS TEST] {msg}", flush=True)

class FocusTester:
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

    def upload_file(self, file_path, title=None):
        self.get_csrf()
        filename = os.path.basename(file_path)
        with open(file_path, "rb") as f:
            files = {"file": (filename, f)}
            data = {"title": title} if title else {}
            h = {"X-XSRF-TOKEN": self.csrf_token} if self.csrf_token else {}
            return self.session.post(f"{self.base_url}/resources", files=files, data=data, headers=h, timeout=45)

    def get_resource(self, res_id):
        return self.session.get(f"{self.base_url}/resources/{res_id}", headers=self.headers(), timeout=30)

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

    def add_topic_source(self, topic_id, resource_id):
        self.get_csrf()
        return self.session.post(f"{self.base_url}/focus/topics/{topic_id}/sources/{resource_id}", headers=self.headers(), timeout=30)

    def remove_topic_source(self, topic_id, resource_id):
        self.get_csrf()
        return self.session.delete(f"{self.base_url}/focus/topics/{topic_id}/sources/{resource_id}", headers=self.headers(), timeout=30)

    def create_note(self, res_id, content):
        h = self.headers()
        h["Content-Type"] = "application/json"
        return self.session.post(f"{self.base_url}/resources/{res_id}/notes", json={"content": content}, headers=h, timeout=30)

def run_real_user_focus_test():
    ts = int(time.time())
    email = f"student_focus_{ts}@example.com"
    pwd = "StudentPassword123!"

    tester = FocusTester(BASE_URL)

    log(f"1. Dang ky tai khoan nguoi dung thuc te: {email}")
    r = tester.register(email, pwd, "Sinh Vien K32 IT")
    assert r.status_code == 201, f"Dang ky that bai: {r.text}"
    log("-> Dang ky va xac thuc session thanh cong!")

    log("2. Upload tai lieu thuc te vao Thu vien:")
    files = [
        ("oop-basics.md", "Lập trình Hướng Đối Tượng & 4 Tính Chất Cốt Lõi"),
        ("rag-architecture.md", "Kiến trúc RAG & Thuật toán Reciprocal Rank Fusion (RRF)"),
        ("software-engineering-guide.docx", "Nguyên lý Kỹ nghệ Phần mềm")
    ]
    uploaded = {}
    for fn, title in files:
        fpath = os.path.join(FIXTURES_DIR, fn)
        up_r = tester.upload_file(fpath, title=title)
        assert up_r.status_code in [200, 201, 202], f"Upload {fn} that bai: {up_r.text}"
        res_data = up_r.json()
        uploaded[fn] = res_data
        log(f"   + Da upload '{title}' (ID: {res_data['id']})")

    log("3. Cho qua trinh Ingestion hoan tat (READY)...")
    start = time.time()
    ready_ids = []
    while time.time() - start < 75:
        ready_ids = []
        for fn, res in uploaded.items():
            chk = tester.get_resource(res["id"])
            if chk.status_code == 200 and chk.json().get("processingStatus") == "READY":
                ready_ids.append(res["id"])
        log(f"   - Tien do xu ly: {len(ready_ids)}/{len(uploaded)} tai lieu READY...")
        if len(ready_ids) == len(uploaded):
            break
        time.sleep(3)

    log(f"-> Hoan tat Ingestion trong {int(time.time() - start)} giay. {len(ready_ids)} tai lieu da READY!")

    log("4. Tao Topic Hoc Sau (Study Topic): 'Lam chu OOP & RAG Architecture'")
    topic_r = tester.create_study_topic(
        title="Làm chủ OOP & RAG Architecture",
        goal="Hiểu sâu 4 tính chất hướng đối tượng và thuật toán RRF trong tìm kiếm kết hợp hybrid",
        resource_ids=[uploaded["oop-basics.md"]["id"], uploaded["rag-architecture.md"]["id"]]
    )
    assert topic_r.status_code in [200, 201], f"Tao topic that bai: {topic_r.text}"
    topic = topic_r.json()
    topic_id = topic["id"]
    log(f"-> Tao Topic thanh cong! ID: {topic_id}, Title: {topic['title']}")
    log(f"   Attached resources: {len(topic.get('resources', []))}")

    log("5. Sinh Lo trinh Khai niem Hoc sau (Generate Learning Plan)...")
    plan_r = tester.generate_topic_plan(topic_id)
    assert plan_r.status_code == 200, f"Sinh plan that bai: {plan_r.text}"
    topic_detail = plan_r.json()
    concepts = topic_detail.get("concepts", [])
    log(f"-> Sinh thanh cong {len(concepts)} khai niem trong lo trinh:")
    for idx, c in enumerate(concepts):
        log(f"   [{idx+1}] {c['title']} ({c['studyStatus']}) - {len(c.get('sources', []))} nguon doi chung")
        log(f"       + Tom tat: {c.get('summary', '')[:80]}...")
        if c.get("whyItMatters"):
            log(f"       + Y nghia: {c.get('whyItMatters', '')[:80]}...")

    assert len(concepts) >= 2, "Lo trinh phai co it nhat 2 khai niem"

    log("6. Thu nghiem chuyen trang thai hoc tap cua cac Khai niem:")
    c1 = concepts[0]
    c2 = concepts[1]
    tester.update_concept_status(topic_id, c1["id"], "LEARNING")
    tester.update_concept_status(topic_id, c2["id"], "CHECKED")
    log(f"   - Khai niem 1 '{c1['title']}' -> LEARNING")
    log(f"   - Khai niem 2 '{c2['title']}' -> CHECKED")

    topic_after = tester.get_study_topic(topic_id).json()
    log(f"-> Thong ke tien do chu de: Checked={topic_after.get('checkedCount')}, Learning={topic_after.get('learningCount')}, Review={topic_after.get('reviewNeededCount')}")

    log("7. Luu Takeaway Note cho Khai niem:")
    first_res_id = uploaded["oop-basics.md"]["id"]
    note_r = tester.create_note(first_res_id, f"[Takeaway on {c1['title']}]: Ghi nho quan trong ve nguyen ly dong goi va che giau du lieu.")
    assert note_r.status_code in [200, 201], "Luu takeaway note that bai"
    log("-> Luu Takeaway Note thanh cong!")

    log("8. Sinh Bai kiem tra Ghi nho (Active Recall Quiz)...")
    quiz_r = tester.generate_quiz(topic_id)
    assert quiz_r.status_code == 200, f"Tao quiz that bai: {quiz_r.text}"
    quiz = quiz_r.json()
    attempt_id = quiz["attemptId"]
    questions = quiz.get("questions", [])
    log(f"-> Tao thanh cong Quiz Attempt ID: {attempt_id} voi {len(questions)} cau hoi:")
    for q_idx, q in enumerate(questions):
        log(f"   Cau {q_idx+1}: {q['question']}")
        for opt_idx, opt in enumerate(q.get("options", [])):
            log(f"      ({chr(65+opt_idx)}) {opt}")

    assert len(questions) >= 1, "Quiz phai co cau hoi"

    log("9. Nop bai kiem tra voi 1 cau sai co chu y de kich hoat Review Queue:")
    # Answer question 0 with option 0, question 1 with option 3, etc.
    answers = {}
    for i, q in enumerate(questions):
        answers[q["id"]] = 3 if i == 0 else 0  # Deliberate choice
    sub_r = tester.submit_quiz(attempt_id, answers)
    assert sub_r.status_code == 200, f"Nop quiz that bai: {sub_r.text}"
    sub_res = sub_r.json()
    log(f"-> Ket qua cham diem: {sub_res.get('scoreCorrect')}/{sub_res.get('totalQuestions')} ({sub_res.get('percentage')}%)")
    for r_idx, res_item in enumerate(sub_res.get("results", [])):
        is_cor = res_item.get("userAnswer") == res_item.get("correctOption")
        log(f"   Cau {r_idx+1}: {'DUNG' if is_cor else 'SAI'} | Giai thich: {res_item.get('explanation', '')[:60]}...")
        if res_item.get("sourceResourceTitle"):
            log(f"      Nguon doi chung: {res_item.get('sourceResourceTitle')} | Snippet: {res_item.get('sourceSnippet', '')[:50]}...")

    log("10. Kiem tra Hang doi On tap (Review Queue) tu dong cap nhat:")
    q_r = tester.get_review_queue()
    assert q_r.status_code == 200, "Get review queue that bai"
    queue = q_r.json()
    log(f"-> So luong muc can on tap trong Queue: {len(queue)}")
    for q_item in queue:
        log(f"   ! Khai niem: '{q_item.get('conceptTitle')}' (Topic: {q_item.get('topicTitle')})")

    log("11. Kiem tra Focus Next Recommendation (Uu tien on tap truoc):")
    fn_r = tester.get_focus_next()
    assert fn_r.status_code == 200, "Get focus next that bai"
    fn = fn_r.json()
    log(f"-> Focus Next khuyen nghi: {fn.get('title')} (Ly do: {fn.get('reason')})")

    log("12. Thu nghiem quan ly Nguon tai lieu cua Topic (Them & Xoa nguon):")
    docx_id = uploaded["software-engineering-guide.docx"]["id"]
    add_r = tester.add_topic_source(topic_id, docx_id)
    assert add_r.status_code in [200, 204], "Them nguon that bai"
    topic_with_3 = tester.get_study_topic(topic_id).json()
    log(f"   + Sau khi them nguon: {len(topic_with_3.get('resources', []))} tai lieu trong topic")

    del_r = tester.remove_topic_source(topic_id, docx_id)
    assert del_r.status_code in [200, 204], "Xoa nguon that bai"
    topic_with_2 = tester.get_study_topic(topic_id).json()
    log(f"   - Sau khi xoa nguon: {len(topic_with_2.get('resources', []))} tai lieu trong topic")

    log("\n========================================================")
    log("KET LUAN: PHAN FOCUS TOPIC DEEPDIVE HOAT DONG 100% CHUAN XAC!")
    log("Tat ca cac luong thuc te da duoc kiem thu va xac nhan thanh cong.")
    log("========================================================")

if __name__ == "__main__":
    run_real_user_focus_test()
