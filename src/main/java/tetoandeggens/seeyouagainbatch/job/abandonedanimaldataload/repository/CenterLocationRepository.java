package tetoandeggens.seeyouagainbatch.job.abandonedanimaldataload.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbatch.domain.CenterLocation;

public interface CenterLocationRepository extends JpaRepository<CenterLocation, Long> {

    List<CenterLocation> findByCenterNoIn(Set<String> centerNos);
}