package tetoandeggens.seeyouagainbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "type")
	private Species type;

	@Column(name = "code", unique = true)
	private String code;

	@Builder
	public BreedType(Long id, String code, String name, Species type) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.type = type;
	}
}
