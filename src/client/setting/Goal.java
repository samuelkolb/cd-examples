package client.setting;

/**
 * Created by samuelkolb on 03/11/15.
 *
 * @author Samuel Kolb
 */
public enum Goal {

	CONSTRAINTS {
		@Override
		public <T> T accept(GoalVisitor<T> visitor) {
			return visitor.visitConstraints();
		}
	}, SOFT_CONSTRAINTS {
		@Override
		public <T> T accept(GoalVisitor<T> visitor) {
			return visitor.visitSoftConstraints();
		}
	}, OPTIMIZATION {
		@Override
		public <T> T accept(GoalVisitor<T> visitor) {
			return visitor.visitOptimization();
		}
	};

	public interface GoalVisitor<T> {
		T visitConstraints();
		T visitSoftConstraints();
		T visitOptimization();
	}

	/**
	 * Accept a visitor
	 * @param visitor	The visitor
	 * @param <T>		The return type
	 * @return			The return value
	 */
	public abstract <T> T accept(GoalVisitor<T> visitor);
}
