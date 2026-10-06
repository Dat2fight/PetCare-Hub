import apiClient from '../api/client';

export interface Service {
    id: number;
    name: string;
    description: string;
    serviceType: 'GROOMING' | 'VETERINARY';
    durationMinutes: number;
    price: number;
    active: boolean;
    createdAt?: string;
}

export const serviceService = {
    getAllServices: async (type?: 'GROOMING' | 'VETERINARY'): Promise<Service[]> => {
        const params = new URLSearchParams();
        if (type) params.append('type', type);
        
        const response = await apiClient.get<Service[]>(`/services?${params.toString()}`);
        return response.data;
    },

    getServiceById: async (id: number): Promise<Service> => {
        const response = await apiClient.get<Service>(`/services/${id}`);
        return response.data;
    }
};

