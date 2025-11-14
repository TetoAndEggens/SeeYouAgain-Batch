package tetoandeggens.seeyouagainbatch.job.animaldataload.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import tetoandeggens.seeyouagainbatch.domain.BreedType;

public interface BreedTypeRepository extends JpaRepository<BreedType, Long> {

    List<BreedType> findByCodeIn(Set<String> codes);
}