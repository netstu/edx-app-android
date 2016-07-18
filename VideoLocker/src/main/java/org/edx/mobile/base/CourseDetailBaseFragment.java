package org.edx.mobile.base;

import com.google.inject.Inject;

import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.logger.Logger;

public abstract class CourseDetailBaseFragment extends BaseFragment {

    @Inject
    protected IEdxEnvironment environment;

    protected final Logger logger = new Logger(getClass().getName());
}
