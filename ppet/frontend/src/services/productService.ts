import apiClient from '../api/client';

export interface Product {
    id: number;
    name: string;
    description: string;
    price: number;
    stockQuantity: number;
    categoryId?: number;
    active: boolean;
    createdAt?: string;
    updatedAt?: string;
    // Frontend specific
    imageUrl?: string;
    rating?: number;
}

export interface ProductPage {
    content: Product[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

export const productService = {
    getAllProducts: async (page = 0, size = 12, categoryId?: number, active?: boolean): Promise<ProductPage> => {
        const params = new URLSearchParams();
        params.append('page', page.toString());
        params.append('size', size.toString());
        if (categoryId !== undefined) params.append('categoryId', categoryId.toString());
        if (active !== undefined) params.append('active', active.toString());

        const response = await apiClient.get<ProductPage>(`/products?${params.toString()}`);
        return response.data;
    },

    getProductById: async (id: number): Promise<Product> => {
        const response = await apiClient.get<Product>(`/products/${id}`);
        return response.data;
    },
};