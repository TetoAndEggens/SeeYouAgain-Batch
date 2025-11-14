package tetoandeggens.seeyouagainbatch.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

@Entity
@Table(name = "animal_location")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnimalLocation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "animal_location_id")
	private Long id;

	@Column(name = "name")
	private String name;

	@Column(name = "address")
	private String address;

	@Column(name = "coordinates", columnDefinition = "POINT SRID 4326", nullable = false)
	private Point coordinates;

	@Column(name = "center_no", unique = true)
	private String centerNo;

	private static final GeometryFactory geometryFactory =
		new GeometryFactory(new PrecisionModel(), 4326);

	@Builder
	public AnimalLocation(Long id, String address, String centerNo,
		Double latitude, Double longitude, String name) {
		this.id = id;
		this.address = address;
		this.centerNo = centerNo;
		this.name = name;
		double lon = (longitude != null) ? longitude : 0.0;
		double lat = (latitude != null) ? latitude : 0.0;
		this.coordinates = createPoint(lon, lat);
	}

	public static Point createPoint(double longitude, double latitude) {
		return geometryFactory.createPoint(new Coordinate(longitude, latitude));
	}
}