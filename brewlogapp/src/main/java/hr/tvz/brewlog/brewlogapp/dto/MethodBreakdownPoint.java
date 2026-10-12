package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Method;

public record MethodBreakdownPoint (Method method, long count){}
