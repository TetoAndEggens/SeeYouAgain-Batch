package tetoandeggens.seeyouagainbatch.job.animaldataload.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbatch.domain.AnimalLocation;

public interface AnimalLocationRepository extends JpaRepository<AnimalLocation, Long> {

    List<AnimalLocation> findByCenterNoIn(Set<String> centerNos);
}