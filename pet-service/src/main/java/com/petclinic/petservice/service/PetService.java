package com.petclinic.petservice.service;

import com.petclinic.petservice.dto.PetDto;
import com.petclinic.petservice.entity.Owner;
import com.petclinic.petservice.entity.Pet;
import com.petclinic.petservice.exception.ResourceNotFoundException;
import com.petclinic.petservice.repository.OwnerRepository;
import com.petclinic.petservice.repository.PetRepository;
import com.petclinic.petservice.kafka.PetEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final PetEventPublisher petEventPublisher;

    @Transactional
    public Pet addPet(PetDto.CreateRequest request) {
        Owner owner = ownerRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ sở hữu ID: " + request.getOwnerId()));
        
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
        
        petEventPublisher.publishPetCreatedEvent(savedPet);
        
        return savedPet;
    }

    @Transactional
    public Pet updatePetHealthInfo(Long petId, Double weight, String allergies) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng ID: " + petId));
        
        if (weight != null) {
            pet.setWeight(weight);
        }
        if (allergies != null) {
            pet.setAllergies(allergies);
        }
        
        return petRepository.save(pet);
    }

    public List<Pet> getPetsByOwner(Long ownerId) {
        return petRepository.findByOwnerId(ownerId);
    }
}
