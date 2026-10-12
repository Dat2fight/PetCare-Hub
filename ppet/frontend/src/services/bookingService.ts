import apiClient from '../api/client';

export interface Appointment {
    id: number;
    petId: number;
    serviceId: number;
    staffId?: number;
    customerId?: number;
    appointmentTime: string; // ISO datetime string
    status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
    notes?: string;
    createdAt?: string;
}

export interface AppointmentCreateRequest {
    petId: number;
    serviceId: number;
    appointmentTime: string; // ISO string
    notes?: string;
}

export const bookingService = {
    getAllAppointments: async (): Promise<Appointment[]> => {
        const response = await apiClient.get<Appointment[]>('/appointments');
        return response.data;
    },

    getAppointmentsByUser: async (userId: number): Promise<Appointment[]> => {
        const response = await apiClient.get<Appointment[]>(`/appointments/user/${userId}`);
        return response.data;
    },

    bookAppointment: async (request: AppointmentCreateRequest): Promise<Appointment> => {
        const response = await apiClient.post<Appointment>('/appointments', request);
        return response.data;
    },

    getAvailableSlots: async (date: string, serviceId: number): Promise<string[]> => {
        const params = new URLSearchParams({
            date: date,
            serviceId: serviceId.toString()
        });
        const response = await apiClient.get<string[]>(`/appointments/slots?${params.toString()}`);
        return response.data;
    }
};

