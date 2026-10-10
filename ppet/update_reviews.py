import re

with open('frontend/src/pages/HomePage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the Card review section
old_review_block = re.search(r'<Card className="bg-gray-50/50 border border-gray-100 p-8 rounded-3xl relative overflow-hidden">.*?</Card>', content, re.DOTALL).group(0)

new_review_block = """<div className="flex gap-6 overflow-x-auto snap-x snap-mandatory pb-4 hide-scrollbar">
              {[
                { name: "Nguyễn Thị Mai", date: "12/04/2025", text: "\\\"Dịch vụ grooming rất chuyên nghiệp, bé nhà mình về rất sạch và thơm. Nhân viên thân thiện, nhiệt tình! Chắc chắn sẽ quay lại ủng hộ PetCare Hub thường xuyên.\\\"", avatar: mockImages.avatar1 },
                { name: "Trần Văn Hùng", date: "05/04/2025", text: "\\\"Mình mua thức ăn cho cún ở đây suốt 1 năm nay, giá cả hợp lý, giao hàng nhanh chóng. Rất hài lòng với chất lượng sản phẩm.\\\"", avatar: mockImages.avatar2 },
                { name: "Lê Minh Tuấn", date: "28/03/2025", text: "\\\"Tính năng tìm kiếm thú cưng phù hợp rất tuyệt vời. Mình đã tìm được một bé Corgi rất ngoan và khỏe mạnh thông qua gợi ý của PetCare Hub.\\\"", avatar: mockImages.placeholder }
              ].map((review, idx) => (
                <Card key={idx} className="bg-gray-50/50 border border-gray-100 p-8 rounded-3xl relative overflow-hidden min-w-full md:min-w-[calc(100%-12px)] snap-center flex-shrink-0">
                  <div className="absolute top-4 right-8 text-8xl text-gray-100 font-serif leading-none">"</div>
                  
                  <div className="flex items-start gap-4 mb-4 relative z-10">
                    <img src={review.avatar} alt="Customer" className="w-12 h-12 rounded-full object-cover shadow-sm" />
                    <div>
                      <h4 className="font-bold text-gray-900">{review.name}</h4>
                      <div className="flex text-secondary-400 text-sm mt-0.5">
                        <FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" /><FiStar className="fill-current" />
                      </div>
                    </div>
                    <span className="ml-auto text-xs text-gray-400 font-medium">{review.date}</span>
                  </div>
                  <p className="text-gray-600 relative z-10 leading-relaxed">
                    {review.text}
                  </p>
                </Card>
              ))}
            </div>"""

content = content.replace(old_review_block, new_review_block)

with open('frontend/src/pages/HomePage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

