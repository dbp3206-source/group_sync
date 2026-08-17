import {
  ArrowUpRight,
  BookOpen,
  BrainCircuit,
  CheckCircle2,
  ChevronDown,
  Compass,
  HelpCircle,
  Network,
  Sparkles,
} from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'

interface GuideSection {
  id: string
  title: string
  icon: typeof Compass
  tag: string
  desc: string
  steps: string[]
  actionLink: string
  actionLabel: string
}

export default function KnowledgeGuidePage() {
  const [activeFaq, setActiveFaq] = useState<number | null>(null)
  const [completedSteps, setCompletedSteps] = useState<Record<number, boolean>>({})

  const toggleStep = (stepIdx: number) => {
    setCompletedSteps(prev => ({ ...prev, [stepIdx]: !prev[stepIdx] }))
  }

  const sections: GuideSection[] = [
    {
      id: 'library',
      title: '1. Thư Viện Tri Thức & Tự Động Phân Loại (Library & Auto-Organize)',
      icon: BookOpen,
      tag: 'QUẢN LÝ DỮ LIỆU',
      desc: 'Hệ thống hỗ trợ nạp đa định dạng và tự động phân loại thông minh ngay khi upload.',
      steps: [
        'Bấm "Import file" trên trang Thư viện để nạp tài liệu PDF, DOCX, TXT hoặc Markdown (hoặc bấm "New note" để tạo ghi chú nhanh).',
        'Cơ chế Auto-Organization tự động quét từ khóa ngữ nghĩa và phân loại tài liệu vào 1 hoặc NHIỀU Collections (chuyên đề) liên quan.',
        'Mỗi tài liệu được tự động bóc tách thành các đoạn văn bản (chunks) và nhúng vector 768 chiều với Gemini Embeddings.',
        'Có thể bấm nút "Auto-Organize" trên thanh công cụ bất kỳ lúc nào để tái phân loại đồng loạt toàn bộ tài nguyên.',
      ],
      actionLink: '/library',
      actionLabel: 'Mở Thư viện Tri thức',
    },
    {
      id: 'ask',
      title: '2. Hỏi Đáp RAG AI & Soi Bằng Chứng Đối Chứng (Ask AI & Grounding)',
      icon: BrainCircuit,
      tag: 'TRUY VẤN THÔNG MINH',
      desc: 'Hỏi đáp chính xác, loại trừ ảo giác AI với trích dẫn bằng chứng và format phân cấp rõ ràng.',
      steps: [
        'Chọn 1 trong 4 phạm vi tra cứu: Tài liệu hiện tại, Chọn các tài liệu cụ thể, Theo Topic/Collection, hoặc Toàn bộ thư viện.',
        'Sử dụng các nút mẫu câu hỏi nhanh (Smart Prompt Pills) bên dưới ô nhập: Lộ trình học tập (Roadmap), So sánh đối chiếu, Tóm tắt 3 ý cốt lõi, Bóc tách công thức, Tạo 5 câu hỏi ôn thi.',
        'Sử dụng Quick Action Bar: Bấm "Sao chép" (góc trên bên phải câu trả lời) để copy toàn bộ Markdown chuẩn.',
        'Soi trích dẫn phát sáng: Nhấp trực tiếp vào số trích dẫn [1], [2] trong câu trả lời để màn hình tự cuộn và phát sáng viền xanh đối chiếu nguồn gốc.',
        'Quan sát trực tiếp luồng suy luận RAG (Pipeline Reasoning Box) ở thanh bên trái với 3 ô cuộn độc lập.',
      ],
      actionLink: '/ask',
      actionLabel: 'Trải nghiệm Hỏi đáp RAG',
    },
    {
      id: 'focus',
      title: '3. Focus Hub: Học Tập Theo Chuyên Đề & Smart Quiz (Topic Deepdive)',
      icon: Sparkles,
      tag: 'HỌC TẬP CHUYÊN SÂU',
      desc: 'Không gian tập trung nghiên cứu theo chủ đề với Reader, bộ tạo Quiz trắc nghiệm và Sơ đồ mạng lưới.',
      steps: [
        'Chọn chuyên đề học tập trên thanh Topic Pills (ví dụ: Database Systems, Software Engineering, AI & Security,...).',
        'Chọn tài liệu bên trái để mở Workspace nghiên cứu chuyên sâu.',
        'Tab Reader: Đọc nội dung bóc tách với phông chữ thoáng đãng và thanh trượt lưu tiến độ đọc (0% - 100%).',
        'Tab Smart Quiz: Bộ câu hỏi trắc nghiệm tự động sinh từ tài liệu; chọn đáp án, bấm "Nộp bài" để nhận phản hồi đúng/sai tức thì kèm giải thích chi tiết.',
        'Thao tác thêm nguồn trực tiếp: Bấm "Thêm tài liệu vào Topic" hoặc "Tạo ghi chú" để tự động lưu vào Library và gắn luôn vào Topic đang chọn.',
      ],
      actionLink: '/focus',
      actionLabel: 'Vào Không gian Focus',
    },
    {
      id: 'graph',
      title: '4. Sơ Đồ Mạng Lưới Liên Kết Tri Thức (Knowledge Graph)',
      icon: Network,
      tag: 'TRỰC QUAN HÓA',
      desc: 'Ngắm nhìn bức tranh toàn cảnh về mối quan hệ giữa các tài liệu, cụm chủ đề và hệ thống.',
      steps: [
        'Chuyển đổi chế độ xem trên trang Thư viện: Bấm [ 🌐 Sơ Đồ Tri Thức ] trên thanh công cụ.',
        'Mã màu trực quan phân loại định dạng: 🔴 PDF, 🔵 DOCX, 🟡 Markdown, 🟣 TXT, 🟢 Ghi chú Note.',
        'Hiệu ứng Aura Glow: Di chuột qua bất kỳ node nào để phát sáng vầng hào quang và xem thẻ tóm tắt chi tiết.',
        'Nhấp chuột vào một Node tài liệu để truy cập trực tiếp vào không gian làm việc của tài liệu đó.',
      ],
      actionLink: '/library',
      actionLabel: 'Khám phá Sơ đồ Tri thức',
    },
  ]

  const faqs = [
    {
      q: 'Một tài liệu có thể thuộc nhiều Collection / Topic cùng lúc được không?',
      a: 'Hoàn toàn được! Kiến trúc cơ sở dữ liệu của KnowledgeOS hỗ trợ mối quan hệ Nhiều - Nhiều (Many-to-Many). Khi một tài liệu chứa nhiều khía cạnh (ví dụ vừa về AI vừa về Security), hệ thống tự động liên kết vào cả 2 Collection tương ứng.',
    },
    {
      q: 'Làm thế nào để đối chiếu câu trả lời của AI với nội dung gốc trong sách/tài liệu?',
      a: 'Mọi câu trả lời đều được gắn các số trích dẫn [1], [2]. Bạn chỉ cần nhấp chuột trực tiếp vào số [1] hoặc [2], trang web sẽ tự động cuộn xuống khối bằng chứng và phát sáng viền xanh để bạn đọc đoạn văn bản gốc.',
    },
    {
      q: 'Làm sao để sao chép câu trả lời của AI sang Notion, Obsidian hoặc Word?',
      a: 'Ở góc trên bên phải mỗi câu trả lời của KNOWLEDGEOS có nút "[ 📋 Sao chép ]". Bấm nút này và dán (Ctrl + V) sang Notion/Obsidian; toàn bộ cấu trúc tiêu đề, danh sách, in đậm và công thức logic sẽ được giữ nguyên vẹn.',
    },
    {
      q: 'Bộ câu hỏi Smart Quiz trong Focus Hub hoạt động như thế nào?',
      a: 'Khi bạn chọn một tài liệu trong Focus Hub và bấm vào tab "Smart Quiz Ôn Tập", hệ thống tự động bóc tách các định lý, công thức và luận điểm cốt lõi để tạo ra bộ đề thi trắc nghiệm kèm giải thích chi tiết cho từng câu hỏi.',
    },
  ]

  return (
    <section className="kos-page kos-guide-page">
      {/* Header */}
      <header className="kos-guide-hero">
        <div className="kos-guide-badge">
          <Compass size={16} /> HƯỚNG DẪN SỬ DỤNG TOÀN DIỆN
        </div>
        <h1>Làm chủ KnowledgeOS trong 5 phút.</h1>
        <p>
          Cẩm nang chi tiết từ A-Z hướng dẫn bạn cách tải tài liệu, tự động phân loại, hỏi đáp đối chứng RAG không ảo giác, ôn tập Smart Quiz và khám phá mạng lưới tri thức.
        </p>
      </header>

      {/* Quick-Start Checklist Banner */}
      <div className="kos-guide-checklist-card">
        <div className="kos-checklist-header">
          <div>
            <h3>🚀 Bắt đầu nhanh (Quick-Start Checklist)</h3>
            <p>4 bước trải nghiệm trọn vẹn sức mạnh của KnowledgeOS</p>
          </div>
          <span className="kos-checklist-progress">
            Đã hoàn thành: {Object.values(completedSteps).filter(Boolean).length} / 4 bước
          </span>
        </div>

        <div className="kos-checklist-grid">
          {[
            {
              title: '1. Nạp tài liệu đầu tiên',
              desc: 'Import 1 file PDF/DOCX hoặc tạo ghi chú mới',
              link: '/library',
              btn: 'Mở Thư viện',
            },
            {
              title: '2. Tự động Phân loại',
              desc: 'Xem tài liệu tự động gán vào Collection & Tags',
              link: '/library',
              btn: 'Xem Collections',
            },
            {
              title: '3. Hỏi đáp RAG AI',
              desc: 'Dùng Smart Prompt Pills & Soi trích dẫn phát sáng',
              link: '/ask',
              btn: 'Thử Hỏi AI',
            },
            {
              title: '4. Ôn tập với Smart Quiz',
              desc: 'Vào Focus Hub và làm bài trắc nghiệm chấm điểm',
              link: '/focus',
              btn: 'Làm Quiz ngay',
            },
          ].map((item, idx) => (
            <div
              key={idx}
              className={`kos-check-item ${completedSteps[idx] ? 'is-completed' : ''}`}
              onClick={() => toggleStep(idx)}
            >
              <div className="kos-check-box">
                <CheckCircle2
                  size={20}
                  className={completedSteps[idx] ? 'kos-check-active' : 'kos-check-idle'}
                />
              </div>
              <div className="kos-check-info">
                <h4>{item.title}</h4>
                <p>{item.desc}</p>
                <Link
                  to={item.link}
                  className="kos-check-link"
                  onClick={e => e.stopPropagation()}
                >
                  {item.btn} <ArrowUpRight size={13} />
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Feature Deepdive Sections */}
      <div className="kos-guide-sections-list">
        {sections.map(sec => {
          const Icon = sec.icon
          return (
            <article key={sec.id} className="kos-guide-section-card">
              <div className="kos-guide-sec-header">
                <div className="kos-sec-icon-wrap">
                  <Icon size={24} />
                </div>
                <div className="kos-sec-title-wrap">
                  <span className="kos-guide-sec-tag">{sec.tag}</span>
                  <h2>{sec.title}</h2>
                  <p>{sec.desc}</p>
                </div>
                <Link to={sec.actionLink} className="kos-button kos-button--primary kos-sec-btn">
                  {sec.actionLabel} <ArrowUpRight size={16} />
                </Link>
              </div>

              <div className="kos-guide-steps-box">
                <h4>💡 Các bước thực hiện chi tiết:</h4>
                <ul>
                  {sec.steps.map((step, sIdx) => (
                    <li key={sIdx}>
                      <span className="kos-step-num">{sIdx + 1}</span>
                      <span>{step}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </article>
          )
        })}
      </div>

      {/* FAQ Section */}
      <div className="kos-guide-faq-card">
        <div className="kos-faq-header">
          <HelpCircle size={22} />
          <h3>Câu Hỏi Thường Gặp (FAQ)</h3>
        </div>

        <div className="kos-faq-list">
          {faqs.map((faq, fIdx) => (
            <div
              key={fIdx}
              className={`kos-faq-item ${activeFaq === fIdx ? 'is-open' : ''}`}
            >
              <button
                type="button"
                className="kos-faq-question"
                onClick={() => setActiveFaq(activeFaq === fIdx ? null : fIdx)}
              >
                <span>{faq.q}</span>
                <ChevronDown size={18} className="kos-faq-arrow" />
              </button>
              {activeFaq === fIdx && (
                <div className="kos-faq-answer">
                  <p>{faq.a}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
