import apiClient from '../api/client';

export interface Product {
    id: number;
    name: string;
    description: string;
    price: number;
    imageUrl?: string;
    categoryId?: number;
    categoryName?: string;
    sku?: string;
    active: boolean;
    stockQuantity?: number;
    createdAt?: string;
    updatedAt?: string;
    // Frontend-only display fields
    rating?: number;
}

export interface ProductCategory {
    id: number;
    name: string;
    description?: string;
    parentId?: number;
}

export interface ProductPage {
    content: Product[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export const productService = {
    getAllProducts: async (page = 0, size = 12, categoryId?: number, active?: boolean, keyword?: string, signal?: AbortSignal): Promise<ProductPage> => {
        const params = new URLSearchParams();
        params.append('page', page.toString());
        params.append('size', size.toString());
        if (categoryId !== undefined) params.append('categoryId', categoryId.toString());
        if (active !== undefined) params.append('active', active.toString());

        if (keyword) params.set('keyword', keyword);
        const response = await apiClient.get<ProductPage>(`/products?${params.toString()}`, { signal });
        return response.data;
    },

    getProductById: async (id: number): Promise<Product> => {
        const response = await apiClient.get<Product>(`/products/${id}`);
        return response.data;
    },

    getAllCategories: async (): Promise<ProductCategory[]> => {
        const response = await apiClient.get<ProductCategory[]>('/categories');
        return response.data;
    },

    addToCart: async (productId: number, quantity: number): Promise<void> => {
        await apiClient.post('/cart/items', { productId, quantity });
    },
};
