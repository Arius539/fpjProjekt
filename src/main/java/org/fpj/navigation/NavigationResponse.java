package org.fpj.navigation;

public record NavigationResponse<T>(T controller, Boolean isLoaded) {}
