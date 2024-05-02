package fr.iut.bc.pkdxapi.controllers;

import org.springframework.web.bind.annotation.RestController;

import fr.iut.bc.pkdxapi.models.Pkmn.Pkmn;
import fr.iut.bc.pkdxapi.models.Pkmn.PkmnData;
import fr.iut.bc.pkdxapi.models.Pkmn.PkmnTypes;
import fr.iut.bc.pkdxapi.models.Pkmn.requests.PkmnRegionRequest;
import fr.iut.bc.pkdxapi.models.Pkmn.requests.PkmnTypesResponse;
import fr.iut.bc.pkdxapi.services.PkmnService;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;



@RestController
@RequestMapping("/pkmn")
public class PkmnController {
    PkmnService pkmnService;
    
    public PkmnController(PkmnService pkmnService) {
        this.pkmnService = pkmnService;
    }
    
    
  
    @GetMapping("")
    public PkmnData getByIdOrName(
        @RequestParam(required = false) Optional<ObjectId> id,
        @RequestParam(required = false) Optional<String> name
    ) {
        if (id.isPresent()) {
            return pkmnService.getById(id.get());
        }

        return pkmnService.getByName(name.get());
    }

      
    @PostMapping("")
    public void create(@RequestBody Pkmn pkmn) {
        pkmnService.create(pkmn);        
    }

    @PutMapping("")
    public PkmnData updatePkmn(
        @RequestParam(required = true) ObjectId id,
        @RequestParam(required = false) Optional<String> name,
        @RequestParam(required = false) Optional<String> description,
        @RequestParam(required = false) Optional<String> imgUrl,
        @RequestParam(required = false) Optional<PkmnTypes> typeOne,
        @RequestParam(required = false) Optional<PkmnTypes> typeTwo
    ) {

        PkmnData pkmnData = pkmnService.updateById(
            id, 
            name,
            description, 
            imgUrl, 
            typeOne, 
            typeTwo
        );   

        return pkmnData;
    }

    @DeleteMapping("")
    public void deleteByIdOrName(
        @RequestParam(required = false) Optional<ObjectId> id,
        @RequestParam(required = false) Optional<String> name
    ) {
        if (id.isPresent()) {
            pkmnService.deleteById(id.get());
        } 

        if (name.isPresent()) {
            pkmnService.deleteByName(name.get());
        }
    }


    @GetMapping("/types")
    public PkmnTypesResponse getPkmnTypes() {
        return pkmnService.getAllPkmnTypes();
    }





    @PostMapping("/region")
    public PkmnData addRegion(@RequestBody PkmnRegionRequest regionRequest) {
        return pkmnService.addRegion(
            regionRequest.getName(), 
            regionRequest.getRegion()
        );
    }

    @DeleteMapping("/region")
    public void removeRegion(
        @RequestParam ObjectId id, 
        @RequestParam String regionName
    ) {
        pkmnService.removeRegion(
            id, 
            regionName
        );
    }



    @GetMapping("/search")
    public Page<PkmnData> searchPkmn(
        @RequestParam(required = false, defaultValue = "") String partialName,
        @RequestParam(required = false) Optional<String> typeOne,
        @RequestParam(required = false) Optional<String> typeTwo,
        @RequestParam(required = false, defaultValue = "0") String page,
        @RequestParam(required = false, defaultValue = "12") String size
    ) {

        return pkmnService.searchPkmn(
            partialName, 
            typeOne, 
            typeTwo, 
            Integer.parseInt(page), 
            Integer.parseInt(size)
        );
    }









    @PostMapping("/api")
    public PkmnData addPokemonsFromApi() {
        return pkmnService.addPokemonsFromApi();
    }
}
