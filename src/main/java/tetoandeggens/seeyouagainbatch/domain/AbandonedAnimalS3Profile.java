package tetoandeggens.seeyouagainbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "ABANDONED_ANIMAL_S3_PROFILE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbandonedAnimalS3Profile extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "abandoned_animal_s3_profile_id")
	private Long id;

	@Column(name = "object_key", unique = true)
	private String objectKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "image_type")
	private ImageType imageType = ImageType.WEBP;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "abandoned_animal_id")
	private AbandonedAnimal abandonedAnimal;

	@Builder
	public AbandonedAnimalS3Profile(String objectKey, AbandonedAnimal abandonedAnimal) {
		this.objectKey = objectKey;
		this.abandonedAnimal = abandonedAnimal;
	}
}