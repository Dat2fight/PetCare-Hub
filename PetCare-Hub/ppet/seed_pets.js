const API_URL = 'http://localhost:8081/api/v1';

async function seed() {
  try {
    // 1. Login as admin
    console.log('Logging in as admin...');
    const loginRes = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'admin',
        password: 'admin123'
      })
    });
    if (!loginRes.ok) throw new Error(await loginRes.text());
    const loginData = await loginRes.json();
    const token = loginData.token;
    console.log('Token received:', token);

    const headers = { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}` 
    };

    // 2. Create a pet
    console.log('Creating a pet...');
    const petRes = await fetch(`${API_URL}/pets`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        name: 'Milo',
        speciesId: 1,
        breedId: 1,
        healthStatus: 'HEALTHY',
        availabilityStatus: 'AVAILABLE',
        ownerId: 1,
        description: 'Very friendly and active',
        imageUrl: 'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=500&q=60'
      })
    });
    if (!petRes.ok) throw new Error(await petRes.text());
    const petData = await petRes.json();
    const petId = petData.id;
    console.log('Pet created with ID:', petId);

    // 3. Create a medical record
    console.log('Creating medical record...');
    const medRes = await fetch(`${API_URL}/medical-records`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        petId: petId,
        veterinarianId: 1,
        examinationDate: '2026-10-01',
        findings: 'Weight is optimal.',
        diagnosis: 'Routine checkup. Healthy.',
        recommendations: 'Keep up the good work.'
      })
    });
    if (!medRes.ok) throw new Error(await medRes.text());
    console.log('Medical record created.');

    // 4. Create a vaccination
    console.log('Creating vaccination...');
    const vacRes = await fetch(`${API_URL}/vaccinations`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        petId: petId,
        veterinarianId: 1,
        vaccineName: 'Rabies',
        vaccineBatchNumber: 'RB-2026-X',
        dateAdministered: '2026-10-01',
        nextDueDate: '2027-10-01',
        notes: 'No side effects observed'
      })
    });
    if (!vacRes.ok) throw new Error(await vacRes.text());
    console.log('Vaccination created.');

    console.log('Seeding completed successfully!');
  } catch (error) {
    console.error('Error seeding data:', error.message);
  }
}

seed();
