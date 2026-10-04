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

    const servingToken = tokens.find(
        t => t.status === 'IN_CONSULTATION'
    );

    const avgServiceTime =
        queue.avgServiceTime || 5;

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

    const waitingAhead = tokens.filter(
        t =>
            t.tokenNumber < targetTokenNumber &&
            t.status === 'WAITING'
    ).length;

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

        remaining =
            elapsed < avgServiceTime
                ? avgServiceTime - elapsed
                : 1;
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
            tokens.length > 0 &&
            tokens[0].tokenNumber === targetTokenNumber &&
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

    if (
        ![
            'IN_CONSULTATION',
            'COMPLETED',
            'CANCELLED'
        ].includes(targetToken.status)
    ) {

        if (!servingToken) {

            eta =
                (
                    completedCount === 0 &&
                    waitingAhead === 0
                )
                    ? 0
                    : waitingAhead * avgServiceTime;

        } else {

            eta =
                remaining +
                (waitingAhead * avgServiceTime);
        }
    }

    eta = Math.round(eta);


    // -----------------------------------------
    // Progress
    // -----------------------------------------

    let progress =
        completedCount +
        (servingToken ? 1 : 0);

    if (targetToken.status === 'IN_CONSULTATION') {
        progress = totalPeople;
    }


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