package tetoandeggens.seeyouagainbatch.common.reader.expression;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;

public enum WhereExpression {
    GT((id, page, currentId) -> page == 0 ? id.goe(currentId) : id.gt(currentId)),
    LT((id, page, currentId) -> page == 0 ? id.loe(currentId) : id.lt(currentId));

    private final WhereNumberFunction number;

    WhereExpression(WhereNumberFunction number) {
        this.number = number;
    }

    public <N extends Number & Comparable<?>> BooleanExpression expression(NumberPath<N> id, int page, N currentId) {
        return this.number.apply(id, page, currentId);
    }
}