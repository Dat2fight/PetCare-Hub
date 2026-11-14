export type CatalogField = {
    key: string;
    label: string;
    type?: string;
    required?: boolean;
    options?: Record<string, string>;
    ref?: string;
    default?: string;
    min?: number;
    max?: number;
};
export type CatalogDefinition = {
    title: string;
    permission: string;
    readPermission?: string;
    fields: CatalogField[];
};
const name: CatalogField = { key: 'name', label: 'Tên', required: true };
const description: CatalogField = { key: 'description', label: 'Mô tả', type: 'textarea' };
const price: CatalogField = { key: 'price', label: 'Giá (đ)', type: 'number', required: true, min: 0 };
const active: CatalogField = { key: 'active', label: 'Đang hoạt động', type: 'boolean', default: 'true' };
const image: CatalogField = { key: 'imageUrl', label: 'URL ảnh HTTPS', type: 'url' };
export const catalogs: Record<string, CatalogDefinition> = {
    products: { title: 'Sản phẩm', permission: 'CATALOG_MANAGE', readPermission: 'CATALOG_READ', fields: [name, { key: 'sku', label: 'Mã SKU', required: true }, description, price, image, { key: 'categoryId', label: 'Danh mục', ref: 'categories', required: true }, active, { key: 'reorderPoint', label: 'Ngưỡng tồn kho thấp (trống = 10)', type: 'number', min: 0 }] },
    categories: { title: 'Danh mục sản phẩm', permission: 'CATALOG_MANAGE', readPermission: 'CATALOG_READ', fields: [name, description, { key: 'parentId', label: 'Danh mục cha', ref: 'categories' }, { key: 'displayOrder', label: 'Thứ tự hiển thị', type: 'number', default: '0', min: 0 }] },
    pets: { title: 'Thú cưng đăng bán', permission: 'SALE_PET_MANAGE', fields: [name, description, { ...price, min: 0.01 }, image, { key: 'speciesId', label: 'Loài', ref: 'species', required: true }, { key: 'breedId', label: 'Giống', ref: 'breeds' }, { key: 'birthDate', label: 'Ngày sinh', type: 'date' }, { key: 'healthStatus', label: 'Sức khỏe', options: { HEALTHY: 'Khỏe mạnh', DUE_FOR_VACCINATION: 'Đến hạn tiêm', HEALTH_WARNING: 'Cảnh báo sức khỏe', UNDER_TREATMENT: 'Đang điều trị', RECOVERING: 'Đang hồi phục' }, default: 'HEALTHY' }, { key: 'availabilityStatus', label: 'Trạng thái bán', options: { AVAILABLE: 'Đang bán', UNAVAILABLE: 'Ngừng bán' }, default: 'AVAILABLE' }] },
    species: { title: 'Loài thú cưng', permission: 'SALE_PET_MANAGE', fields: [name, description] },
    breeds: { title: 'Giống thú cưng', permission: 'SALE_PET_MANAGE', fields: [name, description, { key: 'speciesId', label: 'Loài', ref: 'species', required: true }] },
    services: { title: 'Dịch vụ', permission: 'CATALOG_MANAGE', fields: [name, description, price, { key: 'durationMinutes', label: 'Thời lượng (phút)', type: 'number', min: 5, max: 480, default: '30', required: true }, { key: 'serviceType', label: 'Loại dịch vụ', options: { VETERINARY: 'Thú y', GROOMING: 'Làm đẹp' }, default: 'VETERINARY' }, active] },
    promotions: { title: 'Mã khuyến mãi', permission: 'PROMOTION_MANAGE', fields: [{ key: 'code', label: 'Mã khuyến mãi', required: true }, description, { key: 'discountType', label: 'Loại giảm', options: { PERCENTAGE: 'Phần trăm', FIXED_AMOUNT: 'Số tiền' }, default: 'PERCENTAGE' }, { key: 'discountValue', label: 'Giá trị giảm', type: 'number', required: true, min: 0.01 }, { key: 'minOrderAmount', label: 'Tiền sản phẩm tối thiểu (đ)', type: 'number', required: true, min: 0, default: '0' }, { key: 'maxUses', label: 'Lượt toàn cục (trống = không giới hạn)', type: 'number', min: 1 }, { key: 'maxUsesPerCustomer', label: 'Lượt mỗi khách', type: 'number', min: 1, default: '1' }, { key: 'startDate', label: 'Bắt đầu (giờ cửa hàng)', type: 'datetime-local', required: true }, { key: 'endDate', label: 'Kết thúc (giờ cửa hàng)', type: 'datetime-local', required: true }, active] },
    packages: { title: 'Gói chăm sóc', permission: 'PACKAGE_MANAGE', fields: [name, description, { ...price, min: 0.01 }, { key: 'durationMonths', label: 'Thời hạn (tháng)', type: 'number', min: 1, max: 120, default: '1' }, ...['Grooming', 'Checkup', 'Vaccination'].map((key, i) => ({ key: 'includes' + key, label: ['Có làm đẹp', 'Có khám định kỳ', 'Có tiêm phòng'][i], type: 'boolean', default: 'false' })), active] }
};
export const initialValues = (fields: CatalogField[], data: Record<string, unknown> = {}) => Object.fromEntries(fields.map(f => [f.key, data[f.key] == null ? f.default || '' : String(data[f.key])]));
export const formBody = (fields: CatalogField[], values: Record<string, string>) => Object.fromEntries(fields.map(f => [f.key, values[f.key] === '' ? null : f.type === 'boolean' ? values[f.key] === 'true' : f.type === 'number' || f.ref ? Number(values[f.key]) : values[f.key]]));
