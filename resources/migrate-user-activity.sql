TRUNCATE UserActivity;

INSERT INTO UserActivity (userId, lessonId, complete, countHints, timeTaken, attemptNumber, timestamp, type)
  SELECT
    userId,
    exerciseId,
    complete,
    countHints,
    timeTaken,
    attemptNumber,
    timestamp,
    'answer'
  FROM UserExerciseAnswer;

INSERT INTO UserActivity (userId, lessonId, timestamp, type)
  SELECT
    userId,
    exerciseId,
    timestamp,
    'mastery'
  FROM UserExSingleMastery;

INSERT INTO UserActivity (userId, lessonId, status, position, timestamp, type)
  SELECT
    userId,
    lessonId,
    status,
    position,
    timestamp,
    'view'
  FROM UserVideoView;
