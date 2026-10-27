import apiClient from '../api/client';

export interface PetDTO {
    id: number;
    name: string;
    speciesId?: number;
    speciesName?: string;
    breedId?: number;
    breedName?: string;
    age?: number;
    birthDate?: string;
    price?: number | null;
    healthStatus: string;
    availabilityStatus: string;
    ownerId?: number;
    description?: string;
    imageUrl?: string;
}

export type PublicPetDTO = Omit<PetDTO, 'ownerId' | 'healthStatus'>;
export interface QuickPetCreateRequest { name: string; speciesId: number; age: number }
export interface QuickPetSpecies { id: number; name: string }

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
    createMyPet: async (request: QuickPetCreateRequest): Promise<PetDTO> => (await apiClient.post<PetDTO>('/pets/my-pets', request)).data,
    getQuickPetSpecies: async (signal?: AbortSignal): Promise<QuickPetSpecies[]> => {
        const { data } = await apiClient.get<QuickPetSpecies[]>('/species', { signal });
        return data.filter(s => ['dog', 'cat', 'chó', 'mèo', 'chó (dog)', 'mèo (cat)'].includes(s.name.trim().toLowerCase()));
    },
    getMyPets: async (signal?: AbortSignal): Promise<PetDTO[]> => {
        const response = await apiClient.get<PetDTO[]>('/pets/my-pets', { signal });
        return response.data;
    },
    
    getPetById: async (id: number): Promise<PetDTO> => {
        const response = await apiClient.get<PetDTO>(`/pets/my-pets/${id}`);
        return response.data;
    },

    getMedicalRecords: async (petId: number, page = 0): Promise<MedicalRecordDTO[]> => {
        const response = await apiClient.get<MedicalRecordDTO[]>(`/medical-records/pet/${petId}?page=${page}&size=20`);
        return response.data;
    },

    getVaccinations: async (petId: number, page = 0): Promise<VaccinationDTO[]> => {
        const response = await apiClient.get<VaccinationDTO[]>(`/vaccinations/pet/${petId}?page=${page}&size=20`);
        return response.data;
    }
};

