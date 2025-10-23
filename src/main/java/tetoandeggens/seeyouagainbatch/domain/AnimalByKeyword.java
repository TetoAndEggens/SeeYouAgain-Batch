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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ANIMAL_BY_KEYWORD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalByKeyword extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "animal_by_keyword_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "notification_keyword_id")
	private NotificationKeyword notificationKeyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "abandoned_animal_id")
	private AbandonedAnimal abandonedAnimal;
}
