import apiClient from '../api/client';

export interface PetDTO {
    id: number;
    name: string;
    speciesId?: number;
    speciesName?: string;
    breedId?: number;
    breedName?: string;
    age?: number;
    healthStatus: string;
    availabilityStatus: string;
    ownerId?: number;
    description?: string;
    imageUrl?: string;
}

export interface MedicalRecordDTO {
    id: number;
    petId: number;
    veterinarianId: number;
    veterinarianName: string;
    appointmentId?: number;
    examinationDate: string;
    findings: string;
    diagnosis?: string;
    recommendations?: string;
    createdAt?: string;
}

export interface VaccinationDTO {
    id: number;
    petId: number;
    veterinarianId: number;
    veterinarianName: string;
    vaccineName: string;
    vaccineBatchNumber?: string;
    dateAdministered: string;
    nextDueDate?: string;
    notes?: string;
    createdAt?: string;
}

export const petManagementService = {
    getMyPets: async (): Promise<PetDTO[]> => {
        const response = await apiClient.get<PetDTO[]>('/pets/my-pets');
        return response.data;
    },
    
    getPetById: async (id: number): Promise<PetDTO> => {
        const response = await apiClient.get<PetDTO>(`/pets/${id}`);
        return response.data;
    },

    getMedicalRecords: async (petId: number): Promise<MedicalRecordDTO[]> => {
        const response = await apiClient.get<MedicalRecordDTO[]>(`/medical-records/pet/${petId}`);
        return response.data;
    },

    getVaccinations: async (petId: number): Promise<VaccinationDTO[]> => {
        const response = await apiClient.get<VaccinationDTO[]>(`/vaccinations/pet/${petId}`);
        return response.data;
    }
};

