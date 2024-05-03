package fr.iut.bc.pkdxapi.services;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import fr.iut.bc.pkdxapi.errors.Pkmn.PkmnDoesntExistException;
import fr.iut.bc.pkdxapi.errors.Trainer.TrainerAlreadyExist;
import fr.iut.bc.pkdxapi.errors.Trainer.TrainerDoesntExistException;
import fr.iut.bc.pkdxapi.models.Pkmn.PkmnData;
import fr.iut.bc.pkdxapi.models.Trainer.TrainerData;
import fr.iut.bc.pkdxapi.models.Trainer.request.TrainerRequest;
import fr.iut.bc.pkdxapi.repositories.PkmnRepository;
import fr.iut.bc.pkdxapi.repositories.TrainerRepository;

@Service
public class TrainerService {

    private TrainerRepository repository;
    private PkmnRepository pkmnRepository;

    public TrainerService(TrainerRepository trainerRepository, PkmnRepository pkmnRepository) {
        this.repository = trainerRepository;
        this.pkmnRepository = pkmnRepository;
    }



    public TrainerData getTrainerByUsername(String username) {
        Optional<TrainerData> trainerData = repository.findByUsername(username);

        if (!trainerData.isPresent()) {
            throw new TrainerDoesntExistException("Trainer with username " + username + " doesn't exist");
        }

        return trainerData.get();
    } 

    public TrainerData getTrainerById(ObjectId id) {
        Optional<TrainerData> trainerData = repository.findById(id);

        if (!trainerData.isPresent()) {
            throw new TrainerDoesntExistException("Trainer with id " + id + " doesn't exist");
        }

        return trainerData.get();
    }


    public TrainerData createTrainer(TrainerRequest trainer) {
        String username = getUserName();

        if (trainerExist(username)) {
            throw new TrainerAlreadyExist("Trainer with username " + username + " already exist");
        }

        TrainerData trainerData = new TrainerData(
            username, 
            trainer.getImgUrl(), 
            trainer.getTrainerName(), 
            List.of(),
            List.of()
        );

        return repository.save(trainerData);   
    }

    public void deleteTrainer() {
        String username = getUserName();
        Optional<TrainerData> trainerData = repository.findByUsername(username);

        if (!trainerData.isPresent()) {
            throw new TrainerDoesntExistException("Trainer with username " + username + " doesn't exist");
        }

        repository.delete(trainerData.get());
    }

    public TrainerData updateTrainer( Optional<String> imgUrl, Optional<String> trainerName) {
        String username = getUserName();
        Optional<TrainerData> data = repository.findByUsername(username);

        if (!data.isPresent()) {
            throw new TrainerDoesntExistException("Trainer with username " + username + " doesn't exist");
        }

        TrainerData trainerData = data.get();

        if (imgUrl.isPresent()) {
            trainerData.setImgUrl(imgUrl.get());
        }

        if (trainerName.isPresent()) {
            trainerData.setTrainerName(trainerName.get());
        }

        return repository.save(trainerData);
    }

    public TrainerData updateTrainerPokemon(ObjectId pkmnId, boolean isCaught) {
        Optional<PkmnData> pdata = pkmnRepository.findById(pkmnId);
        if (!pdata.isPresent()) {
            throw new PkmnDoesntExistException("Pokemon with id " + pkmnId + " doesn't exist");
        }

        String username = getUserName();
        Optional<TrainerData> tdata = repository.findByUsername(username);
        if (!tdata.isPresent()) {
            throw new TrainerDoesntExistException("Trainer with username " + username + " doesn't exist");
        }

        TrainerData trainerData = tdata.get();
        PkmnData pkmnData = pdata.get();
        String id = pkmnData.getId();
        
        List<String> pkmnsCaught = trainerData.getPkmnCaught();
        List<String> pkmnsSeen = trainerData.getPkmnSeen();

        if (isCaught) {
            if (addPkmnToTrainer(pkmnsCaught, id)) {
                pkmnsCaught.add(id);
                trainerData.setPkmnCaught(pkmnsCaught);
            }
            if (addPkmnToTrainer(pkmnsSeen, id)) {
                pkmnsSeen.add(id);
                trainerData.setPkmnSeen(pkmnsSeen);
            }
        } else {
            if (addPkmnToTrainer(pkmnsSeen, id)) {
                pkmnsSeen.add(id);
                trainerData.setPkmnSeen(pkmnsSeen);
            }
        }

        return repository.save(trainerData);
    }


    public Page<TrainerData> searchTrainers(String partialName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);  
        return repository.searchTrainers(partialName, pageable);
    }



    private boolean addPkmnToTrainer(List<String> pkmnIds, String id) {
        for (String pkmnId : pkmnIds) {
            if (pkmnId.equals(id)) {
                return false;
            }
        }
        return true;
    }


    private String getUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return username;
    }

    private boolean trainerExist(String username) {
        return repository.findByUsername(username).isPresent();
    }
}
