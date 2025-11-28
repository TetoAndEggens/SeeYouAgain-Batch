package tetoandeggens.seeyouagainbatch.job.keywordmapping.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tetoandeggens.seeyouagainbatch.domain.KeywordCategoryType;
import tetoandeggens.seeyouagainbatch.domain.KeywordType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordCombinationDto {

	private Long id;
	private String keyword;
	private KeywordType keywordType;
	private KeywordCategoryType keywordCategoryType;
}
