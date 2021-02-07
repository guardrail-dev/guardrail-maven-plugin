package com.example.springmvc.server;

import com.example.springmvc.server.petstore.definitions.Pet;
import com.example.springmvc.server.petstore.pet.PetHandler;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Service
public class PetHandlerImpl implements PetHandler {
    @Override
    public CompletionStage<AddPetResponse> addPet(Pet body) {
        return null;
    }

    @Override
    public CompletionStage<UpdatePetResponse> updatePet(Pet body) {
        return null;
    }

    @Override
    public CompletionStage<FindPetsByStatusResponse> findPetsByStatus(List<String> status) {
        return null;
    }

    @Override
    public CompletionStage<FindPetsByStatusEnumResponse> findPetsByStatusEnum(String status) {
        return null;
    }

    @Override
    public CompletionStage<FindPetsByTagsResponse> findPetsByTags(List<String> tags) {
        return null;
    }

    @Override
    public CompletionStage<UpdatePetWithFormResponse> updatePetWithForm(long petId, Optional<String> name, Optional<String> status) {
        return null;
    }

    @Override
    public CompletionStage<GetPetByIdResponse> getPetById(long petId) {
        return null;
    }

    @Override
    public CompletionStage<DeletePetResponse> deletePet(long petId, Optional<String> apiKey, Optional<Boolean> includeChildren, Optional<String> status) {
        return null;
    }

    @Override
    public CompletionStage<UploadFileResponse> uploadFile(long petId, Optional<String> additionalMetadata, Optional<MultipartFile> file, MultipartFile file2, MultipartFile file3, long longValue, long customValue, Optional<Long> customOptionalValue) {
        return null;
    }
}
