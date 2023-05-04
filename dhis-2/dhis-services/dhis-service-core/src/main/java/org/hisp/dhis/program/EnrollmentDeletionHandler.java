/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.program;

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.stereotype.Component;

/**
 * @author Quang Nguyen
 */
@Component
@RequiredArgsConstructor
public class EnrollmentDeletionHandler extends IdObjectDeletionHandler<Enrollment>
{
    private final EnrollmentService enrollmentService;

    @Override
    protected void registerHandler()
    {
        whenDeleting( TrackedEntityInstance.class, this::deleteTrackedEntityInstance );
        whenVetoing( Program.class, this::allowDeleteProgram );
        whenDeleting( Program.class, this::deleteProgram );
    }

    private void deleteTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance )
    {
        for ( Enrollment enrollment : trackedEntityInstance.getEnrollments() )
        {
            enrollmentService.deleteEnrollment( enrollment );
        }
    }

    private DeletionVeto allowDeleteProgram( Program program )
    {
        if ( program.isWithoutRegistration() )
        {
            return ACCEPT;
        }
        String sql = "select 1 from programinstance where programid = :id limit 1";
        return vetoIfExists( VETO, sql, Map.of( "id", program.getId() ) );
    }

    private void deleteProgram( Program program )
    {
        Collection<Enrollment> enrollments = enrollmentService.getEnrollments( program );

        if ( enrollments != null )
        {
            Iterator<Enrollment> iterator = enrollments.iterator();
            while ( iterator.hasNext() )
            {
                Enrollment enrollment = iterator.next();
                iterator.remove();
                enrollmentService.hardDeleteEnrollment( enrollment );
            }
        }
    }
}