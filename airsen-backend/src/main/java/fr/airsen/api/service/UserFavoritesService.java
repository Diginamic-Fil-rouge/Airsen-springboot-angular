package fr.airsen.api.service;

import fr.airsen.api.dto.request.AddFavoriteRequest;
import fr.airsen.api.dto.response.UserFavoriteResponse;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.UserFavorite;
import fr.airsen.api.entity.UserFavoriteId;
import fr.airsen.api.exception.DuplicateFavoriteException;
import fr.airsen.api.exception.MaximumFavoritesExceededException;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.UserFavoriteRepository;
import fr.airsen.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user favorite communes.
 *
 * Provides business logic for:
 * - Adding favorites with validation (max 10, no duplicates)
 * - Removing favorites
 * - Retrieving user favorites with complete commune hierarchy
 * - Checking favorite existence
 */
@Service
@Transactional
public class UserFavoritesService {

    private static final Logger logger = LoggerFactory.getLogger(UserFavoritesService.class);
    private static final int MAX_FAVORITES_PER_USER = 10;

    private final UserFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CommuneRepository communeRepository;

    public UserFavoritesService(
            UserFavoriteRepository favoriteRepository,
            UserRepository userRepository,
            CommuneRepository communeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.communeRepository = communeRepository;
    }

    /**
     * Get all favorites for a user with complete commune hierarchy.
     *
     * Uses JOIN FETCH in repository to load commune, department, and region
     * in a single query, preventing N+1 query problems.
     *
     * Results are ordered by creation date (newest first).
     *
     * @param userId User ID
     * @return List of favorites with denormalized commune data
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public List<UserFavoriteResponse> getUserFavorites(Long userId) {
        logger.debug("Fetching favorites for user ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            logger.warn("User not found with ID: {}", userId);
            throw new ResourceNotFoundException(String.format("User not found with ID: %d", userId));
        }

        List<UserFavorite> favorites = favoriteRepository.findByUserId(userId);
        logger.info("Found {} favorites for user ID: {}", favorites.size(), userId);

        return favorites.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Add a commune to user favorites.
     *
     * Business Rules Enforced:
     * 1. User must exist
     * 2. Commune must exist
     * 3. User cannot have more than 10 favorites
     * 4. Duplicate favorites are prevented
     *
     * @param userId User ID
     * @param request Add favorite request with commune INSEE code
     * @return Created favorite response
     * @throws ResourceNotFoundException if user or commune not found
     * @throws MaximumFavoritesExceededException if user has 10 favorites
     * @throws DuplicateFavoriteException if commune already favorited
     */
    public UserFavoriteResponse addFavorite(Long userId, AddFavoriteRequest request) {
        logger.debug("Adding favorite for user ID: {} - commune: {}", userId, request.communeInseeCode());

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userId);
                    return new ResourceNotFoundException(String.format("User not found with ID: %d", userId));
                });

        // Validate commune exists
        Commune commune = communeRepository.findByInseeCode(request.communeInseeCode())
                .orElseThrow(() -> {
                    logger.warn("Commune not found with INSEE code: {}", request.communeInseeCode());
                    return new ResourceNotFoundException(
                            String.format("Commune not found with INSEE code: %s", request.communeInseeCode()));
                });

        // Check maximum favorites limit (efficient count query)
        int currentCount = favoriteRepository.countByUserId(userId);
        if (currentCount >= MAX_FAVORITES_PER_USER) {
            logger.warn("User ID {} attempted to exceed maximum {} favorites (current: {})",
                    userId, MAX_FAVORITES_PER_USER, currentCount);
            throw new MaximumFavoritesExceededException(
                    String.format("Maximum %d favorites per user exceeded", MAX_FAVORITES_PER_USER));
        }

        // Check duplicate using INSEE code directly (no ID conversion needed)
        UserFavoriteId id = new UserFavoriteId(userId, request.communeInseeCode());
        if (favoriteRepository.existsById(id)) {
            logger.warn("User ID {} attempted to add duplicate favorite: {}", userId, request.communeInseeCode());
            throw new DuplicateFavoriteException(
                    String.format("Commune '%s' is already in favorites", commune.getName()));
        }

        // Create and save favorite (UserFavorite constructor uses commune.getInseeCode())
        UserFavorite favorite = new UserFavorite(user, commune);

        UserFavorite saved = favoriteRepository.save(favorite);
        logger.info("Successfully added favorite for user ID {} - commune: {} ({})",
                userId, commune.getName(), request.communeInseeCode());

        return mapToResponse(saved);
    }

    /**
     * Remove a commune from user favorites.
     *
     * Uses efficient DELETE query without loading the entity first.
     * Uses INSEE code directly.
     *
     * @param userId User ID
     * @param communeInseeCode Commune INSEE code (5-digit official identifier)
     * @throws ResourceNotFoundException if favorite not found
     */
    public void removeFavorite(Long userId, String communeInseeCode) {
        logger.debug("Removing favorite for user ID: {} - commune: {}", userId, communeInseeCode);

        // Validate commune exists using AIRSEN's findByInseeCode pattern
        if (!communeRepository.findByInseeCode(communeInseeCode).isPresent()) {
            logger.warn("Commune not found with INSEE code: {}", communeInseeCode);
            throw new ResourceNotFoundException(
                    String.format("Commune not found with INSEE code: %s", communeInseeCode));
        }

        // Check favorite exists using INSEE code directly (no ID lookup needed)
        UserFavoriteId id = new UserFavoriteId(userId, communeInseeCode);
        if (!favoriteRepository.existsById(id)) {
            logger.warn("Favorite not found for user ID {} - commune: {}", userId, communeInseeCode);
            throw new ResourceNotFoundException(
                    String.format("Favorite not found for user ID %d and commune INSEE code %s", userId, communeInseeCode));
        }

        // Delete using INSEE code directly (aligns with AIRSEN's architecture)
        favoriteRepository.deleteByUserIdAndCommuneInseeCode(userId, communeInseeCode);
        logger.info("Successfully removed favorite for user ID {} - commune: {}", userId, communeInseeCode);
    }

    /**
     * Check if a commune is favorited by a user.
     *
     * Fast boolean check without loading entities.
     * Used by frontend to show/hide "Add to favorites" button.
     * Uses INSEE code directly (AIRSEN's architectural pattern).
     *
     * @param userId User ID
     * @param communeInseeCode Commune INSEE code (5-digit official identifier)
     * @return true if commune is in user's favorites
     */
    @Transactional(readOnly = true)
    public boolean isFavorited(Long userId, String communeInseeCode) {
        // Direct existence check using INSEE code (no entity loading needed)
        boolean favorited = favoriteRepository.existsById_UserIdAndId_CommuneInseeCode(userId, communeInseeCode);
        logger.debug("Favorite check for user ID {} - commune: {} - result: {}",
                userId, communeInseeCode, favorited);
        return favorited;
    }

    /**
     * Get the count of favorites for a user.
     *
     * Useful for displaying "X/10 favorites" in UI.
     *
     * @param userId User ID
     * @return Number of favorites (0-10)
     */
    @Transactional(readOnly = true)
    public int getFavoriteCount(Long userId) {
        int count = favoriteRepository.countByUserId(userId);
        logger.debug("Favorite count for user ID {}: {}", userId, count);
        return count;
    }

    /**
     * Remove all favorites for a user.
     *
     * Used when user account is deleted or user wants to clear all favorites.
     * Typically called by UserService during account deletion.
     *
     * @param userId User ID
     */
    public void removeAllFavorites(Long userId) {
        logger.debug("Removing all favorites for user ID: {}", userId);
        int count = favoriteRepository.countByUserId(userId);

        if (count > 0) {
            favoriteRepository.deleteAllByUserId(userId);
            logger.info("Removed {} favorites for user ID: {}", count, userId);
        } else {
            logger.debug("No favorites to remove for user ID: {}", userId);
        }
    }

    /**
     * Maps UserFavorite entity to response DTO.
     *
     * Denormalizes the geographic hierarchy for frontend convenience.
     * Assumes commune, department, and region are loaded via JOIN FETCH.
     * Uses INSEE code (not internal database ID) for stability and interoperability.
     *
     * @param favorite UserFavorite entity with loaded relationships
     * @return UserFavoriteResponse DTO with commune INSEE code
     */
    private UserFavoriteResponse mapToResponse(UserFavorite favorite) {
        Commune commune = favorite.getCommune();

        return new UserFavoriteResponse(
                commune.getInseeCode(),  // Official INSEE code, not internal database ID
                commune.getName(),
                commune.getDepartment().getName(),
                commune.getDepartment().getRegion().getName(),
                favorite.getCreatedAt()
        );
    }
}
