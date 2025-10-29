package tetoandeggens.seeyouagainbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ABANDONED_ANIMAL_PROFILE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbandonedAnimalProfile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "abandoned_animal_profile_id")
	private Long id;

	@Column(name = "popfile1")
	private String popfile1;

	@Column(name = "popfile2")
	private String popfile2;

	@Column(name = "popfile3")
	private String popfile3;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "abandoned_animal_id")
	private AbandonedAnimal abandonedAnimal;

	@Builder
	public AbandonedAnimalProfile(String popfile1, String popfile2, String popfile3, AbandonedAnimal abandonedAnimal) {
		this.popfile1 = popfile1;
		this.popfile2 = popfile2;
		this.popfile3 = popfile3;
		this.abandonedAnimal = abandonedAnimal;
	}
}
