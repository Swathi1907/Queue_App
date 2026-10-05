async function calculateETA(queue, targetTokenNumber) {

    const tokens = queue.tokens || [];

    const targetToken = tokens.find(
        t => t.tokenNumber === targetTokenNumber
    );

    if (!targetToken) {
        return {
            error: "Token not found in queue"
        };
    }

    // -----------------------------------------
    // Current serving token
    // -----------------------------------------

    const servingToken = tokens.find(
        t => t.status === 'IN_CONSULTATION'
    );

    // Average consultation time in minutes
    const avgServiceTime =
    Math.ceil(queue.avgServiceTime || 5);
    // -----------------------------------------
    // Queue statistics
    // -----------------------------------------

    const activeCount = tokens.filter(
        t =>
            ['WAITING', 'IN_CONSULTATION']
                .includes(t.status)
    ).length;

    const totalPeople = tokens.filter(
        t => t.status !== 'CANCELLED'
    ).length;

    const completedCount = tokens.filter(
        t => t.status === 'COMPLETED'
    ).length;

    // People waiting BEFORE target token
    const waitingAhead = tokens.filter(
        t =>
            t.tokenNumber < targetTokenNumber &&
            t.status === 'WAITING'
    ).length;

    // People currently ahead of target
    const peopleAhead = tokens.filter(
        t =>
            t.tokenNumber < targetTokenNumber &&
            ['WAITING', 'IN_CONSULTATION']
                .includes(t.status)
    ).length;


    // -----------------------------------------
    // Remaining time for current consultation
    // -----------------------------------------

    let remaining = 0;

    if (servingToken?.consultationStartedAt) {

        const elapsed =
            (
                Date.now() -
                new Date(
                    servingToken.consultationStartedAt
                ).getTime()
            ) / (1000 * 60);

        remaining = Math.max(
            0,
            avgServiceTime - elapsed
        );
    }


    // -----------------------------------------
    // Queue Status
    // -----------------------------------------

    let queue_status;

    if (targetToken.status === 'IN_CONSULTATION') {

        queue_status = "SERVING";

    } else if (
        ['COMPLETED', 'CANCELLED']
            .includes(targetToken.status)
    ) {

        queue_status = targetToken.status;

    } else if (!servingToken) {

        if (
            completedCount === 0 &&
            waitingAhead === 0
        ) {

            queue_status = "WAITING_TO_START";

        } else {

            queue_status =
                peopleAhead === 0
                    ? "WAITING_FOR_NEXT_CALL"
                    : "WAITING";
        }

    } else {

        queue_status =
            peopleAhead === 0
                ? "NEXT"
                : "WAITING";
    }


    // -----------------------------------------
    // ETA
    // -----------------------------------------

    let eta = 0;

    // ETA is needed only for waiting patients
    if (
        ![
            'IN_CONSULTATION',
            'COMPLETED',
            'CANCELLED'
        ].includes(targetToken.status)
    ) {

        if (!servingToken) {

            // Nobody is currently being served.
            // Only people before the target matter.
            eta =
                waitingAhead * avgServiceTime;

        } else {

            // Current consultation remaining time
            // + estimated time for people waiting ahead
            eta =
                remaining +
                (waitingAhead * avgServiceTime);
        }
    }

    // Never return negative ETA
    eta = Math.max(
        0,
        Math.ceil(eta)
    );


    // -----------------------------------------
    // Progress
    // -----------------------------------------

    const progress =
        completedCount +
        (servingToken ? 1 : 0);


    // -----------------------------------------
    // Return
    // -----------------------------------------

    return {

        eta,

        queue_status,

        avgServiceTime,

        activeCount,

        totalPeople,

        peopleAhead,

        waitingAhead,

        remaining,

        progress,

        currentMember: servingToken,

        servingMember: servingToken
    };
}


module.exports = {
    calculateETA
};