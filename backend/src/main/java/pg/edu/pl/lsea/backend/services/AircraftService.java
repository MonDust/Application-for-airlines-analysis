package pg.edu.pl.lsea.backend.services;

import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pg.edu.pl.lsea.backend.controllers.dto.AircraftResponse;
import pg.edu.pl.lsea.backend.controllers.dto.mapper.AircraftToResponseMapper;

import pg.edu.pl.lsea.backend.entities.*;
import pg.edu.pl.lsea.backend.entities.original.Aircraft;
import pg.edu.pl.lsea.backend.entities.original.Flight;
import pg.edu.pl.lsea.backend.repositories.*;
import pg.edu.pl.lsea.backend.repositories.original.AircraftRepo;
import pg.edu.pl.lsea.backend.repositories.original.FlightRepo;
import pg.edu.pl.lsea.backend.utils.ResourceNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class responsible for aircraft REST API business logic (part of MVC architecture)
 * It gets and saves data to/from AircraftRepo.
 */
@Service
@Transactional
public class AircraftService {
    private final OperatorRepo operatorRepo;
    private final ModelRepo modelRepo;
    private final AircraftRepo aircraftRepo;
    private final AircraftToResponseMapper aircraftToResponseMapper;
    private final FlightRepo flightRepo;

    /**
     * Constructor of the class.
     * @param aircraftRepo - aircraft repository
     * @param aircraftToResponseMapper - mapper
     * @param operatorRepo - operators repository
     */
    public AircraftService(AircraftRepo aircraftRepo, AircraftToResponseMapper aircraftToResponseMapper, OperatorRepo operatorRepo, ModelRepo modelRepo, FlightRepo flightRepo) {
        this.aircraftRepo = aircraftRepo;
        this.aircraftToResponseMapper = aircraftToResponseMapper;
        this.operatorRepo = operatorRepo;
        this.modelRepo = modelRepo;
        this.flightRepo = flightRepo;
    }

    /**
     * Returns the list of all aircrafts stored in database (used for GET request)
     * @return List of AircraftResponse (AircraftResponse = DTO) is what should be exposed via REST API endpoint
     */
    public List<AircraftResponse> getAll() {
        return aircraftRepo.findAll()
                .stream()
                .map(aircraftToResponseMapper)
                .toList();
    }

    /**
     * Returns one particular aircraft stored in database (used for GET request)
     * @return AircraftResponse (=DTO) is what should be exposed via REST API endpoint
     */
    public AircraftResponse getByIcao(String icao) {
        return aircraftRepo.findByIcao24(icao)
                .map(aircraftToResponseMapper)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "icao24", icao));
    }

    /**
     * Checks if the aircraft request is valid
     */
    private boolean checkRequestValidity(AircraftResponse req) {
        if (Objects.equals(req.operator(), "") || Objects.equals(req.model(), "") || Objects.equals(req.owner(), "")) {
            return false;
        }

        if (Objects.equals(req.operator(), "NULL") || Objects.equals(req.model(), "NULL") || Objects.equals(req.owner(), "NULL")) {
            return false;
        }

        return !Objects.equals(req.operator(), null) && !Objects.equals(req.model(), null) && !Objects.equals(req.owner(), null);
    }

    /**
     * Check if aircraft exists.
     * @param icao24 - ICAO
     * @return - boolean true if exists, false if not.
     */
    private boolean checkIfAircraftExists(String icao24) {
        Optional<Aircraft> existingAircraft = aircraftRepo.findByIcao24(icao24);
        return existingAircraft.isPresent();
    }

    /**
     * Creates a particular aircraft and stores it in the database (used for POST request).
     * @return AircraftResponse (=DTO) is what should be exposed via REST API endpoint. It can be ignored.
     */
    public AircraftResponse create(AircraftResponse request) {

        if (!checkRequestValidity(request)) {
            return null;
        }

        // Check if the aircraft already exists
        if (checkIfAircraftExists(request.icao24())) {
            return null;
        }

        // Find or create Operator
        Optional<Operator> existingOperator = operatorRepo.findByName(request.operator());
        Operator operator;
        if (existingOperator.isEmpty()) {
            operator = new Operator(request.operator());
            operatorRepo.save(operator);
        }
        else {
            operator = existingOperator.get();
        }

        // Find or create Model
        Optional<Model> existingModel = modelRepo.findByName(request.model());
        Model model;
        if (existingModel.isEmpty()) {
            model = new Model(request.model());
            modelRepo.save(model);
        }
        else {
            model = existingModel.get();
        }

        Aircraft aircraft = new Aircraft(
                request.icao24(),
                model,
                operator,
                request.owner()
        );

        aircraftRepo.save(aircraft);
        return aircraftToResponseMapper.apply(aircraft);
    }

    /**
     * Creates a list of aircrafts and stores them in the database (used for POST request).
     * It enables client to upload all aircrafts at once.
     * @return List of AircraftResponse (=DTO) is what should be exposed via REST API endpoint. It can be ignored.
     */
    public List<AircraftResponse> createBulk(List<AircraftResponse> request) {
        List<AircraftResponse> validRequests = request.stream()
                .filter(this::checkRequestValidity)
                .toList();

        // Lookups
        Map<String, Operator> operatorMap = operatorRepo.findAll().stream()
                .collect(Collectors.toMap(Operator::getName, o -> o));

        Map<String, Model> modelMap = modelRepo.findAll().stream()
                .collect(Collectors.toMap(Model::getName, m -> m));

        List<Operator> newOperators = new ArrayList<>();
        List<Model> newModels = new ArrayList<>();

        List<Aircraft> aircrafts = new ArrayList<>();
        for (AircraftResponse a : validRequests) {
            if (a.operator() == null || a.model() == null) continue;

            Operator operator = operatorMap.get(a.operator());
            if (operator == null) {
                operator = new Operator(a.operator());
                operatorMap.put(a.operator(), operator);
                newOperators.add(operator);
            }

            Model model = modelMap.get(a.model());
            if (model == null) {
                model = new Model(a.model());
                modelMap.put(a.model(), model);
                newModels.add(model);
            }

            Aircraft newAircraft = new Aircraft(a.icao24(), model, operator, a.owner());
            aircrafts.add(newAircraft);
        }

        Set<String> allIcao24s = aircraftRepo.findAllIcao24();
        List<Aircraft> newAircrafts = aircrafts.stream()
                .filter(a -> !allIcao24s.contains(a.getIcao24()))
                .toList();

        try {
            operatorRepo.saveAll(newOperators);
            modelRepo.saveAll(newModels);
            aircraftRepo.saveAll(newAircrafts);
        } catch (DataIntegrityViolationException ex) {
            System.err.println("Error saving : " + ex.getMessage());
        }

        Set<String> icaoSet = newAircrafts.stream()
                .map(Aircraft::getIcao24)
                .collect(Collectors.toSet());

        List<Flight> relatedFlights = flightRepo.findAllByIcao24In(icaoSet); // Custom method
        Map<String, List<Flight>> flightsByIcao = relatedFlights.stream()
                .collect(Collectors.groupingBy(Flight::getIcao24));

        for (Aircraft aircraft : newAircrafts) {
            List<Flight> related = flightsByIcao.get(aircraft.getIcao24());
            if (related != null) {
                for (Flight flight : related) {
                    aircraft.getFlights().add(flight);
                    flight.setAircraft(aircraft);
                }
            }
        }

        // Save updated flights with aircraft references
        flightRepo.saveAll(relatedFlights);

        return aircrafts.stream()
                .map(aircraftToResponseMapper)
                .toList();
    }



    /**
     * Replaces an aircraft stored in the database (used for PUT request)
     * @param icao24 a unique id of an aircraft - used to find an aircraft that should be replaced
     * @param request an aircraft - it needs all parameters specified
     * @return AircraftResponse (=DTO) is what should be exposed via REST API endpoint.
     */
    public AircraftResponse updateAircraft(String icao24, AircraftResponse request) {
        Aircraft aircraft = aircraftRepo.findByIcao24(icao24)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "icao24", icao24));

        // TODO - Verify correctness of method:
        // Fix model and operator update - remove new Operator(request.operator()) and same for model

        updateModel(aircraft, request.model());
        updateOperator(aircraft, request.operator());
        aircraft.setOwner(request.owner());

        try {
            aircraftRepo.save(aircraft);
        } catch (DataIntegrityViolationException ex) {
            System.err.println("Error saving aircraft: " + ex.getMessage());
        }
        return aircraftToResponseMapper.apply(aircraft);
    }

    /**
     * Updates an aircraft stored in the database (used for PATCH request)
     * @param icao24 a unique id of an aircraft - used to find an aircraft that should be updated
     * @param request an aircraft - you can specify any number of parameters and their values
     * @return AircraftResponse (=DTO) is what should be exposed via REST API endpoint.
     */
    public AircraftResponse patchAircraft(String icao24, AircraftResponse request) {
        Aircraft aircraft = aircraftRepo.findByIcao24(icao24)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "icao24", icao24));

        if (request.model() != null) {
            updateModel(aircraft, request.model());
        }
        if (request.operator() != null) {
            updateOperator(aircraft, request.operator());
        }
        if (request.owner() != null) aircraft.setOwner(request.owner());

        try {
            aircraftRepo.save(aircraft);
        } catch (DataIntegrityViolationException ex) {
            System.err.println("Error saving aircraft: " + ex.getMessage());
        }
        return aircraftToResponseMapper.apply(aircraft);
    }

    private void updateModel(Aircraft aircraft, String modelName) {
        Model model;
        Optional<Model> existingModel = modelRepo.findByName(modelName);
        if (existingModel.isPresent()) {
            model = existingModel.get();
            aircraft.setModel(model);
            model.getAircrafts().add(aircraft);
        }
        else {
            model = new Model(modelName);
            model.getAircrafts().add(aircraft);
            modelRepo.save(model);
            aircraft.setModel(model);
        }
    }

    private void updateOperator(Aircraft aircraft, String operatorName) {
        Operator operator;
        Optional<Operator> existingOperator = operatorRepo.findByName(operatorName);
        if (existingOperator.isPresent()) {
            operator = existingOperator.get();
        }
        else {
            operator = new Operator(operatorName);
            operatorRepo.save(operator);
        }
        aircraft.setOperator(operator);
        operator.getAircrafts().add(aircraft);
    }

    /**
     * Deletes an aircraft based on icao24
     * @param icao24 a unique id of an aircraft
     */
    public void deleteAircraft(String icao24) {
        Aircraft aircraft = aircraftRepo.findByIcao24(icao24)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", "icao24", icao24));

        aircraftRepo.delete(aircraft);
    }

}
