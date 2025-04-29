package ro.unibuc.hello.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import ro.unibuc.hello.data.GameEntity;
import ro.unibuc.hello.data.GameRepository;
import ro.unibuc.hello.dto.Game;
import ro.unibuc.hello.exception.EntityNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GamesService {
    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private MeterRegistry metricsRegistry;

    public List<Game> getAllGames(){
        List<GameEntity> entities = gameRepository.findAll();
        metricsRegistry.counter("games.get.all").increment();
        return convert(entities);
    }

    public List<Game> getGamesAvailable(int tier){
        List<GameEntity> entities = gameRepository.findByTierBetween(0, tier + 1);
        
        return convert(entities);
    }

    public List<Game> getGamebyId(String id){
        Optional<GameEntity> entity = gameRepository.findById(id);
        metricsRegistry.counter("games.get.id").increment();

        List<Game> response = new ArrayList();

        if(!entity.isPresent()){
            response.add(new Game());         
        }else{
            GameEntity game = entity.get();
            response.add(new Game(game.getId(), game.getTitle(), game.getTier()));
        }

        return response;
    }

    public List<Game> getGamesByTitle(String title){
        List<GameEntity> entities = gameRepository.findByTitleLike(title);
        metricsRegistry.counter("games.get.title").increment();
        return convert(entities);
    }

    public List<Game> getGamesByTitleAndTier(String title, int tier){
        List<GameEntity> entities = gameRepository.findByTitleLikeAndTierBetween(title, 0, tier + 1);
        metricsRegistry.counter("games.get.id_and_title").increment();
        return convert(entities);
    }

    public Game saveGame(Game game){
        GameEntity entity = new GameEntity();
        metricsRegistry.counter("games.save").increment();

        entity.setTier(game.getTier());
        entity.setTitle(game.getTitle());
        gameRepository.save(entity);
        return new Game(entity.getId(), entity.getTitle(), entity.getTier());
    }

    public boolean deleteGame(String id){
        Optional<GameEntity> entity = gameRepository.findById(id);
        metricsRegistry.counter("games.delete").increment();

        if(entity.isPresent()){
            gameRepository.delete(entity.get());
            return true;
        }

        return false;
    }

    public void deleteAllGames(){
        gameRepository.deleteAll();
    }

    private List<Game> convert(List<GameEntity> entities){
        return entities.stream()
            .map(entity -> new Game(entity.getId(), entity.getTitle(), entity.getTier()))
            .collect(Collectors.toList());
    }
}
