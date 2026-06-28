package com.github.silent.samurai.speedy.models;

import com.github.silent.samurai.speedy.interfaces.SpeedyConstants;
import com.github.silent.samurai.speedy.interfaces.query.PageInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageInfoImpl implements PageInfo {

    private int pageSize = SpeedyConstants.defaultPageSize;
    private int pageNo = 0;
}
