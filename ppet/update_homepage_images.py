import re

with open('frontend/src/pages/HomePage.tsx', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix Whiskas image
content = re.sub(
    r'name:\s*"Thức ăn Whiskas",.*?image:\s*mockImages\.[^,]+',
    'name: "Thức ăn Whiskas", desc: "1.2kg | Cho mèo trưởng thành", price: "180.000đ", oldPrice: "220.000đ", image: mockImages.foodWhiskas',
    content
)

# Fix Cát vệ sinh image
content = re.sub(
    r'name:\s*"Cát vệ sinh cho mèo",.*?image:\s*mockImages\.[^,]+',
    'name: "Cát vệ sinh cho mèo", desc: "Khử mùi tốt, an toàn", price: "120.000đ", image: mockImages.catLitter',
    content
)

# Fix Bàn chải lông image
content = re.sub(
    r'name:\s*"Bàn chải lông",.*?image:\s*mockImages\.[^,]+',
    'name: "Bàn chải lông", desc: "Phù hợp mọi giống chó mèo", price: "85.000đ", image: mockImages.brush',
    content
)

with open('frontend/src/pages/HomePage.tsx', 'w', encoding='utf-8') as f:
    f.write(content)

