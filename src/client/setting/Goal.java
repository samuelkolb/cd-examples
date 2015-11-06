package client.setting;

/**
 * Represents different kinds of goals.
 *
 * @author Samuel Kolb
 */
@SuppressWarnings("unused")
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

	@SuppressWarnings("JavaDoc")
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
	@SuppressWarnings("UnusedReturnValue")
	public abstract <T> T accept(GoalVisitor<T> visitor);
}
