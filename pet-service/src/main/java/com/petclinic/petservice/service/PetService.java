package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.PetDto;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.BadRequestException;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.outbox.OutboxService;
import com.petclinic.petservice.repository.OwnerRepository;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.security.AccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final OutboxService outboxService;
    private final AccessGuard accessGuard;

    @Transactional
    public PetDto.Response addPet(PetDto.CreateRequest request) {
        Owner owner = findOwner(request.getOwnerId());
        // a customer can only add pets to their own profile
        accessGuard.checkOwnerAccess(owner);

        Pet pet = Pet.builder()
                .name(request.getName())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .isNeutered(request.getIsNeutered())
                .weight(request.getWeight())
                .allergies(request.getAllergies())
                .notes(request.getNotes())
                .owner(owner)
                .build();

        Pet savedPet = petRepository.save(pet);

        // Published to Kafka by OutboxRelay only after this transaction commits
        // (publishing here sent PET_CREATED even when the insert was rolled back)
        outboxService.enqueuePetCreated(savedPet);

        return PetDto.Response.from(savedPet);
    }

    /** Staff only (enforced on the controller). */
    @Transactional
    public PetDto.Response updatePetHealthInfo(Long petId, Double weight, String allergies) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + petId));

        if (weight != null) {
            if (weight <= 0) {
                throw new BadRequestException("Weight must be greater than 0");
            }
            pet.setWeight(weight);
        }
        if (allergies != null) {
            pet.setAllergies(allergies);
        }

        return PetDto.Response.from(pet);
    }

    @Transactional(readOnly = true)
    public List<PetDto.Response> getPetsByOwner(Long ownerId) {
        accessGuard.checkOwnerAccess(findOwner(ownerId));
        return petRepository.findByOwnerId(ownerId).stream().map(PetDto.Response::from).toList();
    }

    private Owner findOwner(Long ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + ownerId));
    }
}
