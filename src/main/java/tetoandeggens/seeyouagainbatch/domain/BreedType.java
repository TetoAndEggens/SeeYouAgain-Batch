package tetoandeggens.seeyouagainbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "BREED_TYPE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BreedType extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "breed_type_id")
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "type")
	private String type;
}
