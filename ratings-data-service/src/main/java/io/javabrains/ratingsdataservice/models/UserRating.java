package io.javabrains.ratingsdataservice.models;

import java.util.List;

public class UserRating {

	private List<Rating> userRating;

	public List<Rating> getUserRating() {
		return userRating;
	}

	public void setUserRating(List<Rating> userRating) {
		this.userRating = userRating;
	}
	
	public UserRating userRating(List<Rating> userRating) {
		this.userRating = userRating;
		return this;
	}
	
	
}
