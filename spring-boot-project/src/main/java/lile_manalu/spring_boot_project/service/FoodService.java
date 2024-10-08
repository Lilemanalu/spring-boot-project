package lile_manalu.spring_boot_project.service;

import lile_manalu.spring_boot_project.entity.AddOn;
import lile_manalu.spring_boot_project.entity.Food;
import lile_manalu.spring_boot_project.model.*;
import lile_manalu.spring_boot_project.repository.AddOnRepository;
import lile_manalu.spring_boot_project.repository.FoodRepository;
import lile_manalu.spring_boot_project.repository.OutletRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FoodService {

    private static final Logger logger = LoggerFactory.getLogger(FoodService.class);

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private AddOnRepository addOnRepository;

    @Autowired
    private OutletRepository outletRepository;

    public CreateFoodResponse create(CreateFoodRequest request) {
        logger.debug("Request to create food: {}", request);

        Food food = new Food();
        food.setId(UUID.randomUUID().toString());
        food.setOutletId(request.getOutlet_id());
        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setPrice(request.getPrice());

        Food savedFood = foodRepository.save(food);
        logger.info("Created new food item: {}", savedFood);

        List<AddOnResponse> addOnResponses = new ArrayList<>();

        // If there are add-ons, create and save them
        if (request.getAddOns() != null && !request.getAddOns().isEmpty()) {
            for (AddOnRequest addOnRequest : request.getAddOns()) {
                AddOn addOn = new AddOn();
                addOn.setId(UUID.randomUUID().toString());
                addOn.setFoodId(food.getId());
//                addOn.setFood(savedFood);
                addOn.setName(addOnRequest.getName());
                addOn.setDescription(addOnRequest.getDescription());
                addOn.setPrice(addOnRequest.getPrice());

                AddOn savedAddOn = addOnRepository.save(addOn);
                logger.info("Created new add-on: {}", savedAddOn);

                addOnResponses.add(
                        AddOnResponse.builder()
                                .id(savedAddOn.getId())
                                .food_id(savedAddOn.getFoodId())
                                .name(savedAddOn.getName())
                                .description(savedAddOn.getDescription())
                                .price(savedAddOn.getPrice())
                                .build()
                );
            }
        }

        return CreateFoodResponse.builder()
                .id(savedFood.getId())
                .outlet_id(savedFood.getOutletId())
                .name(savedFood.getName())
                .price(savedFood.getPrice())
                .description(savedFood.getDescription())
                .addOns(addOnResponses)
                .build();
    }

    //get food by food Id
    public FoodResponse get(String id) {
        logger.debug("Fetching food item with ID: {}", id);

        Food food = foodRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Food item not found with ID: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Food item not found");
                });

        FoodResponse response = toFoodResponse(food);
        logger.debug("Retrieved food item: {}", response);

        return response;
    }

    // Service method to get all food items for a specific outlet
    public List<FoodResponse> list(String outletId) {
        logger.debug("Fetching food items for outlet ID: {}", outletId);

        List<Food> foods = foodRepository.findByOutletId(outletId);

        if (foods.isEmpty()) {
            logger.error("No food items found for outlet ID: {}", outletId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No food items found for this outlet");
        }

        // Convert the list of Food entities to FoodResponse DTOs
        List<FoodResponse> foodResponses = foods.stream()
                .map(this::toFoodResponse)
                .collect(Collectors.toList());

        logger.info("Fetched {} food items for outlet ID: {}", foodResponses.size(), outletId);
        return foodResponses;
    }


    public FoodResponse update(UpdateFoodRequest request){
        logger.debug("Received update request: {}", request);

        Food food = foodRepository.findById(request.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));

        food.setOutletId(request.getOutlet_id());
        food.setName(request.getName());
        food.setDescription(request.getDescription());
        food.setPrice(request.getPrice());
        foodRepository.save(food);

        logger.info("Updated food item: {}", food);

        return toFoodResponse(food);
    }

    public void delete(String foodId) {
        try {
            Food food = foodRepository.findById(foodId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));

            foodRepository.delete(food);
            logger.info("Successfully deleted food item with ID: {}", foodId);
        } catch (ResponseStatusException e) {
            logger.error("Error deleting food item with ID: {}. Error: {}", foodId, e.getReason());
            throw e;
        }
    }

    public List<FoodResponse> list() {
        List<Food> foods = foodRepository.findAll();
        List<FoodResponse> foodResponses = foods.stream()
                .map(this::toFoodResponse)
                .collect(Collectors.toList());

        logger.info("Fetched {} food items", foodResponses.size());
        return foodResponses;
    }

    private FoodResponse toFoodResponse(Food food){
        return FoodResponse.builder()
                .id(food.getId())
                .outlet_id(food.getOutletId())
                .name(food.getName())
                .description(food.getDescription())
                .price(food.getPrice())
                .build();
    }

}
