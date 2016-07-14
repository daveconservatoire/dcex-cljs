-- phpMyAdmin SQL Dump
-- version 4.4.10
-- http://www.phpmyadmin.net
--
-- Host: localhost:8889
-- Generation Time: Jul 12, 2016 at 04:55 AM
-- Server version: 5.5.42
-- PHP Version: 5.6.10

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `dcsite-pre`
--

-- --------------------------------------------------------

--
-- Table structure for table `Course`
--

CREATE TABLE `Course` (
  `id` int(11) NOT NULL,
  `title` varchar(150) NOT NULL,
  `urltitle` varchar(150) NOT NULL,
  `author` varchar(150) NOT NULL,
  `description` varchar(500) NOT NULL,
  `homepage_order` int(11) NOT NULL
) ENGINE=MyISAM AUTO_INCREMENT=8 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Course`
--

INSERT INTO `Course` (`id`, `title`, `urltitle`, `author`, `description`, `homepage_order`) VALUES
(4, 'Reading Music', 'reading-music', 'David Rees', 'How can learning to read some lines and squiggles help me learn other people''s music?  Find out here!', 0),
(7, 'Music:  A Beginner''s Guide', 'music-theory', 'David Rees', 'Love music? Want to learn about how it works? This is the place to start!', 0);

-- --------------------------------------------------------

--
-- Table structure for table `Lesson`
--

CREATE TABLE `Lesson` (
  `filetype` varchar(1) NOT NULL DEFAULT 'l',
  `id` int(11) NOT NULL,
  `seriesno` int(11) NOT NULL,
  `topicno` int(11) NOT NULL,
  `lessonno` int(11) NOT NULL,
  `title` varchar(150) NOT NULL,
  `description` varchar(4000) NOT NULL,
  `keywords` varchar(200) NOT NULL,
  `urltitle` varchar(150) NOT NULL,
  `youtubeid` varchar(70) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=MyISAM AUTO_INCREMENT=167 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Lesson`
--

INSERT INTO `Lesson` (`filetype`, `id`, `seriesno`, `topicno`, `lessonno`, `title`, `description`, `keywords`, `urltitle`, `youtubeid`, `timestamp`) VALUES
('l', 9, 7, 13, 58, 'Percussion', '', '', 'percussion', 'xRTIieYlGqM', '0000-00-00 00:00:00'),
('l', 10, 4, 19, 2, 'Staff and Clefs', '', '', 'staffandclefs', '0kQ9CZcWdCk', '0000-00-00 00:00:00'),
('p', 11, 7, 13, 59, 'Percussion Playlist', '', '', 'percussion-playlist', '', '0000-00-00 00:00:00'),
('l', 57, 4, 19, 9, 'Ledger Lines Continued', '', '', 'ledgerlinescontinued', '5ZPL30NQgnQ', '0000-00-00 00:00:00'),
('l', 13, 7, 13, 56, 'String Instruments', '', '', 'string-instruments', 'L9MMam_pj7s', '0000-00-00 00:00:00'),
('p', 15, 7, 13, 57, 'Strings Playlist', '', '', 'strings-playlist', '', '0000-00-00 00:00:00'),
('l', 16, 7, 13, 60, 'Wind Instruments', '', '', 'wind-instruments', 'G_7CnZGreHw', '0000-00-00 00:00:00'),
('p', 18, 7, 13, 61, 'Wind Playlist', '', '', 'wind-playlist', '', '0000-00-00 00:00:00'),
('l', 19, 7, 13, 62, 'Keyboard Instruments', '', '', 'keyboard-instruments', 'DpSFAAE52HQ', '0000-00-00 00:00:00'),
('p', 20, 7, 13, 63, 'Keyboard Playlist', '', '', 'keyboard-playlist', '', '0000-00-00 00:00:00'),
('l', 21, 7, 15, 85, 'Chamber Ensembles', '', '', 'chamber-ensembles', 'rgJ8qFYPvwY', '0000-00-00 00:00:00'),
('p', 22, 7, 15, 86, 'Chamber Ensembles Playlist', '', '', 'chamber-ensembles-playlist', '', '0000-00-00 00:00:00'),
('l', 23, 7, 15, 83, 'The Orchestra', '', '', 'the-orchestra', 'Z3_5jC5qwIw', '0000-00-00 00:00:00'),
('p', 24, 7, 15, 84, 'The Orchestra Playlist', '', '', 'the-orchestra-playlist', '', '0000-00-00 00:00:00'),
('l', 25, 7, 15, 87, 'Modern Ensembles', '', '', 'modern-ensembles', 'XXj9joGOJS8', '0000-00-00 00:00:00'),
('p', 26, 7, 15, 88, 'Modern Ensembles Playlist', '', '', 'modern-ensembles-playlist', '', '0000-00-00 00:00:00'),
('l', 27, 7, 10, 48, 'Form in European Music', '', '', 'western-forms', 'TFs5yvzc7Sw', '0000-00-00 00:00:00'),
('l', 28, 7, 10, 50, 'Form in American Music', '', '', 'american-forms', 'lBkzmMMC7yY', '0000-00-00 00:00:00'),
('l', 29, 7, 0, 91, 'European Music History', '', '', 'euro-music-history', 'x6ev4z6pZ_4', '0000-00-00 00:00:00'),
('p', 30, 7, 0, 92, 'European Music History Playlist', '', '', 'euro-music-playlist', '', '0000-00-00 00:00:00'),
('p', 32, 7, 10, 51, 'American Music Forms Playlist', '', '', 'usa-forms-playlist', '', '0000-00-00 00:00:00'),
('l', 33, 7, 15, 89, 'American Music History', '', '', 'usa-music-history', '9f91UGqqkto', '0000-00-00 00:00:00'),
('p', 34, 7, 17, 90, 'American Music History Playlist', '', '', 'usa-history-playlist', '', '0000-00-00 00:00:00'),
('l', 158, 7, 9, 46, 'Transposing the Modes', 'Sometimes we''d like to be able to move the modes around to be based on different pitches, a dorian mode starting on G, for example.  \r\n\r\nIn lesson we learn that''s not quite as tough as it sounds!', 'modes, transposition, scale degrees', 'transposing-modes', '-SWEL2LyPkU', '2013-06-02 12:39:52'),
('p', 36, 7, 0, 93, '"What Next?" Playlist', '', '', 'what-next', '', '0000-00-00 00:00:00'),
('p', 45, 7, 10, 49, 'European Forms Playlist', '', '', 'european-forms-playlist', '', '0000-00-00 00:00:00'),
('p', 38, 7, 5, 21, 'Beat Playlist', '', '', 'beat-playlist', '', '0000-00-00 00:00:00'),
('p', 39, 7, 5, 19, 'Rhythm Playlist', '', '', 'rhythm-playlist', '', '0000-00-00 00:00:00'),
('p', 41, 7, 2, 2, 'What is Music? Playlist', '', '', 'what-is-music-playlist', '', '0000-00-00 00:00:00'),
('p', 42, 7, 3, 9, 'Pitch Playlist', '', '', 'pitch-playlist', '', '0000-00-00 00:00:00'),
('p', 47, 7, 7, 32, 'Harmony Playlist', '', '', 'harmony-playlist', '', '0000-00-00 00:00:00'),
('e', 48, 4, 19, 3, 'Exercise: Reading the Treble Clef', '', '', 'treble-clef-reading', 'treble-clef-reading', '0000-00-00 00:00:00'),
('l', 49, 4, 19, 4, 'The Bass Clef', '', '', 'bassclef', 'htM489ztcZI', '0000-00-00 00:00:00'),
('l', 50, 4, 19, 6, 'The Grand Staff', '', '', 'thegrandstaff', 'Nlup-VDbjI0', '0000-00-00 00:00:00'),
('l', 51, 4, 20, 10, 'Introducing Rhythm Notation', '', '', 'rhythmnotation ', 'lt36RA10QDA', '0000-00-00 00:00:00'),
('l', 52, 4, 24, 21, 'Dynamics', '', '', 'dynamics', 'qGlSzKxs3ng', '0000-00-00 00:00:00'),
('e', 53, 4, 19, 5, 'Exercise: Reading the Bass Clef', '', '', 'bass-clef-reading', 'bass-clef-reading', '0000-00-00 00:00:00'),
('e', 54, 4, 19, 7, 'Exercise: Reading the Grand Staff', '', '', 'grand-staff-reading', 'grand-staff-reading', '0000-00-00 00:00:00'),
('l', 55, 4, 26, 23, 'Introduction to Tempo Markings', '', '', 'tempomarkings', 'jZbHfDg94X8', '0000-00-00 00:00:00'),
('l', 56, 4, 19, 8, 'Ledger Lines', '', '', 'legderlines', '1QSoMv93htE', '0000-00-00 00:00:00'),
('l', 58, 4, 20, 25, 'Dotted Notes', '', '', 'dottednotes', '_7DJVSIbDKk', '0000-00-00 00:00:00'),
('l', 59, 4, 18, 1, 'Getting Started', '', '', 'readinggettingstarted ', 'Vg9ioFZLwj0', '0000-00-00 00:00:00'),
('l', 60, 4, 20, 15, 'European Note Names', '', '', 'europeannotenames ', 'Yn_V-zGGIgo', '0000-00-00 00:00:00'),
('e', 66, 4, 20, 16, 'Exercise: European Note Names Quiz', '', '', 'euro-names', 'euro-names', '0000-00-00 00:00:00'),
('e', 62, 4, 20, 12, 'Exercise: Rhythm Reading', '', '', 'rhythm-reading', 'rhythm-reading', '0000-00-00 00:00:00'),
('e', 63, 4, 20, 11, 'Exercise: Rhythm Maths', '', '', 'rhythm-maths', 'rhythm-maths', '0000-00-00 00:00:00'),
('e', 67, 4, 26, 24, 'Exercise: Tempo Markings Quiz', '', '', 'tempo-markings', 'tempo-markings', '0000-00-00 00:00:00'),
('e', 65, 4, 24, 22, 'Exercise: Dynamics Quiz', '', '', 'dynamics-quiz', 'dynamics-quiz', '0000-00-00 00:00:00'),
('l', 68, 4, 20, 26, 'Introducing Rests', '', '', 'rests', '0zraQP5FKQE', '0000-00-00 00:00:00'),
('e', 69, 4, 20, 27, 'Reading Rests', '', '', 'rests-reading', 'rests-reading', '0000-00-00 00:00:00'),
('l', 70, 4, 20, 13, 'Ties', '', '', 'ties', 'CUzH-ASMclQ', '0000-00-00 00:00:00'),
('l', 71, 4, 20, 14, 'Eighth and Sixteenth Notes', '', '', 'eighthssixteenths', '7acwScVgGeE', '0000-00-00 00:00:00'),
('l', 72, 4, 20, 17, 'The Rhythm Tree', '', '', 'rhythmtree', 'xvvIOtA3qbo', '0000-00-00 00:00:00'),
('l', 73, 4, 27, 28, 'Sharps and Flats', '', '', 'sharpsandflats', 'zuszuEwAXjE', '0000-00-00 00:00:00'),
('l', 74, 4, 27, 29, 'Accidental Rules', '', '', 'accidentalrules', 'pzFwrxdw63k', '0000-00-00 00:00:00'),
('l', 79, 4, 20, 18, 'The Story So Far!', '', '', 'thestorysofar', 'uNwmVLm1lck', '0000-00-00 00:00:00'),
('l', 80, 4, 23, 34, 'Introducing Guitar Tablature', '', '', 'introducing-guitar-tab', 'h8X4tMABCXo', '0000-00-00 00:00:00'),
('l', 81, 4, 21, 19, 'Introducing Measures and Bars', '', '', 'introducing-measures-and-bars', 'iXOnLU5M7Z0', '0000-00-00 00:00:00'),
('l', 82, 4, 21, 20, 'Time Signatures', '', '', 'time-signatures', 'gX3IJ7ibFZ8', '0000-00-00 00:00:00'),
('l', 83, 4, 22, 30, 'Introducing Key Signatures', '', '', 'introducing-key-signatures', 'e5rhABjt4G4', '0000-00-00 00:00:00'),
('l', 84, 4, 22, 31, 'Key Signatures with Sharps', '', '', 'sharp-keys', '-a38KtgmgiE', '0000-00-00 00:00:00'),
('l', 85, 4, 22, 32, 'Key Signatures with Flats', '', '', 'flat-keys', 'WXHmhYUkm78', '0000-00-00 00:00:00'),
('l', 86, 7, 2, 1, 'What is Music Theory? ', 'Music theory is really important if we are to understand how music works. In this video Dave introduces some of the key elements that we will be exploring in this music theory course including: pitch, rhythm, harmony, timbre and texture. \r\n\r\nThis is the perfect place to start for a beginner who hasn''t ever considered music theory before.  \r\n\r\nExcited? Well, let''s get started and begin exploring how music works!', 'beginning, what is music theory', 'what-is-music-theory', '5Of7ccsPkmw', '0000-00-00 00:00:00'),
('l', 87, 4, 27, 33, 'The Natural Sign', '', '', 'natural-sign', '85RhsSlDYWE', '0000-00-00 00:00:00'),
('l', 88, 7, 3, 3, 'Pitch and Octaves', 'Pitch refers to how high or low a sound is.  We have a good sense of what high and low pitched sound are, even if we''ve not spoken about them in this way.  \r\n\r\nAll sounds are created when an object vibrates and the pitch of a sound depends on the pitch of this vibration.  The faster the vibration the higher in pitch the sound.  \r\n\r\nIf we double the speed of vibration (or frequency) of a sound then something interesting happens.  The two pitches will sound similar and will blend together well.  This special relationship is known as an octave and is a central part of almost all music around the world.  \r\n\r\n\r\n', 'pitch, frequency, what is pitch, octaves, octave, octave relationship', 'pitch-and-octaves', 'aDutr6g8SeY', '0000-00-00 00:00:00'),
('l', 89, 7, 3, 8, 'The 12 Pitches ', 'In Western music, we split the octave up into 12 different pitches.  These twelve pitches are used to create almost all of our music.  \r\n\r\nOne of the best ways to learn about how we think about these twelve pitches is to look at the piano keyboard where they are clearly laid out for us.  \r\n\r\nThe first thing you might notice is that the white and black keys follow a certain pattern and this helps us to understand how the 12 basic pitches are organised.  The white notes on the piano are named after the first seven letters of the alphabet - A B C D E F G.  \r\n\r\nThe black notes fill in the pitches between these letter named notes.  We talk about these notes as being - flats and sharps.  ', 'note names, letter names for music notes, piano keyboard, piano layout', '12-pitches', 'nVkeCH3GA9s', '0000-00-00 00:00:00'),
('l', 90, 7, 3, 10, 'Tones and Semitones', 'A useful way to measure pitch is to work out how far apart two notes are in terms of their pitch. \r\n\r\nWe already know about octaves as one measure of pitch, but what about for smaller steps up and down in pitch. \r\n\r\nTo measure these distances we can talk about tones and semitones.  On a keyboard instrument like a piano or organ each of the keys is one semitone apart.  The distance between C and C#, for example, is one semitone.  \r\n\r\nA tone is simply a distance in pitch of two tones.  F to G, for example is a distance of a tone.  ', 'tones, semitones, pitch, interval, interval recognition', 'tones-and-semitone', 'hPou3ube_Rg', '0000-00-00 00:00:00'),
('l', 91, 7, 4, 11, 'Scales: The Major Scale', 'If we were to use all twelve pitches of Western music all the time, our music might sound a bit confused and overwhelming.  That''s because we tend to use pitches in smaller groups called scales. \r\n\r\nThe pitches of a scale tend to work well together and can be used to give our music a particular feel or character.  \r\n\r\nMost of the music we tend to hear is based on either a major or minor scale.  In this lesson, we have a look at the major scale, what is sounds like and how we can build one for ourselves.  ', 'major scale, tonality, key', 'major-scales', 'zNDOVQ4H6l4', '0000-00-00 00:00:00'),
('l', 92, 7, 4, 15, 'Whole Tone Scales', 'A whole tone scale is very useful to know about.  It is often used to produce a dreamy, fantasy-like character to music and is used in film and television sound tracks to indicate moving from one dimension to another - a flashback or dream sequence, for example. \r\n\r\nA whole tone scale is really easy to build as well.  Instead of a combination of semitone and tone steps, like the major and minor scales, this scale is just a series of tones.  This means that actually there are only two different versions of the whole tone scale, making it very easy to remember.', 'whole tone scales, dream sequence, flashback', 'wholetonescales', 'ztv0s1KDck4', '0000-00-00 00:00:00'),
('l', 93, 7, 4, 12, 'The Natural Minor Scale', 'Sometimes we want to express darker emotions in music and the major scale isn''t going to cut it.  We might consider using minor scales instead.  \r\n\r\nMinor scales sound darker and more sorrowful than major scales.  There are few different flavours of minor scale that we tend to use.  \r\n\r\nIn this video we have a look at the natural minor scale.', 'natural minor, minor scale, relative minor', 'natural-minor', 'zFqlHWMXW0Y', '0000-00-00 00:00:00'),
('l', 94, 7, 4, 13, 'The Harmonic Minor Scale', 'Another flavour of minor scale is the harmonic minor scale.  It will be very useful to know about when later we try and add harmony to pieces using a minor scale.  \r\n\r\nIt is a good idea, just like with any scale to try and learn what this scale looks like and sounds like starting on a twelve different pitches.', 'harmonic minor scale, minor scale, minor harmony', 'harmonic-minor', 'tFA8GVaCgsk', '0000-00-00 00:00:00'),
('l', 95, 7, 4, 14, 'The Melodic Minor Scale', 'You might find that the harmonic minor scale is a bit angular at the top of the scale to write smooth melodies.  This is where the melodic minor scale comes in handy.  It has a slightly different pattern of notes at the top of the scale which some composers have found easier to use to write melodies. \r\n\r\nBut, if you''re composing, you''re in the driving seat, you should use what sounds good to you!  Mozart composed melodies with both the harmonic and melodic flavours of minor scale.', 'melodic minor scale, minor scale, composing melodies, Mozart', 'melodic-minor', 'HX7jlDCI2JM', '0000-00-00 00:00:00'),
('l', 96, 7, 5, 18, 'Getting Started with Rhythm', 'What is rhythm?  Well, rhythm is a very broad term to describe the way we organise sounds in time.  One of the special things about music is that it takes place over a set period of time and to really master music we need to develop our ability to measure out sounds over a particular period of time.\r\n\r\nIt is rhythm that keeps an army marching in step together and also makes us want to dance.  It is a very powerful tool, but also very simple.', 'rhythm, marching, time, meter, beat, musical rhythm', 'rhythm-intro', 'gozEpt59MIE', '0000-00-00 00:00:00'),
('l', 97, 7, 4, 16, 'Scale Degrees', 'Now we know a few scales, it will be a good idea to learn about scale degrees.  Scale degrees help us count the steps of a scale as it goes from start to end.  \r\n\r\nFor example, in the key of C major, the first scale degree with be a C, the third scale degree will be an E and the fifth scale degree will be a G.', 'scale degrees, scale steps', 'scale-degrees', 'GlJ6TxVKcDU', '0000-00-00 00:00:00'),
('l', 98, 7, 4, 17, 'Scale Degree Names', 'Now we know about scale degrees, it might be a good idea to get to learn their names.  That''s right, every step of a major and minor scale has a special name.  \r\n\r\nThey might seem a bit difficult to remember at first, but you''ll get there in the end.  Every scale starts on the tonic note (1) and is then followed by the supertonic (2), mediant (3), subdominant (4), dominant (5), submediant (6) and leading note (7).  \r\n\r\nI remember these by using the mnemonic - "To see more sheep, don''t say lunch", the first letters of each of these words matches up with - tonic, supertonic, mediant, subdominant, dominant, submediant and leading note.', 'names of scale degrees, tonic, supertonic, mediant, submediant, subdominant, dominant, leading note', 'scale-degree-names', 'fOHIIDlRR3o', '0000-00-00 00:00:00'),
('l', 99, 7, 5, 20, 'What is beat?', 'Almost all the music we hear is built on the foundation of a beat.  A music''s beat helps us to measure out how long or short sounds in the music should be and how they should be arranged.\r\n\r\nEven when music is slow and it isn''t always obvious, there will normally still beat ticking along under the music.  Beats tend to be regular and steady even when the music sounds uneven and irregular.\r\n\r\nIn this video, we have a go at measuring some different beats.', 'beat, pulse, time signature, beats per minute', 'what-is-beat', '9vdinCLSE7I', '0000-00-00 00:00:00'),
('l', 100, 7, 5, 22, 'Meet the Metronome', 'You''ve probably seen a metronome before.  There are old ones with a pendulum that swings from side to side and new digital ones that beep.  But all of these devices are designed to help you establish a regular and steady beat.  \r\n\r\nSometimes musicians find it useful to practice their playing along with a metronome to make sure that they aren''t rushing ahead or lagging behind the beat.  \r\n\r\nAfter lots of practice musicians tend to develop their own internal sense of time and beat which keeps their playing in time. ', 'rhythm, metronome, timing, musical time, music practice, playing in time', 'meet-the-metronome', 'ZYLKuoCeBK0', '0000-00-00 00:00:00'),
('l', 101, 7, 5, 23, 'Counting in Beats', 'Have you ever head someone count in a piece of music with "One and a two and a".  Why are they doing this?  \r\n\r\nThey''re doing what is called counting in.  It helps all of the musicians in a group play together it also reveals something special about how musicians create their music. \r\n\r\nCounting in beats is a how a musician can measure out sounds, and as such create a specific rhythm.\r\n\r\nIf we can count beats, we can measure a sound that is one beat, two beats or three beats long, for example.', 'counting beats, rhythm, playing together, ensemble', 'counting-in-beats', '2u-IP3j818c', '0000-00-00 00:00:00'),
('l', 102, 7, 5, 24, 'Let''s Make Some Rhythms', 'Now it is time to make some rhythms of our own.  This can be as simple as just one long sound.\r\n\r\nIn this video we''ll have a go at measuring out some sounds to make simple rhythms and hopefully afterwards you''ll feel confident to try composing some simple rhythms of your own!', 'rhythm, composition, beginners rhythm', 'making-rhythms', '7TxRMoNcQzU', '0000-00-00 00:00:00'),
('l', 103, 7, 5, 27, 'Building a Drumbeat', 'A good way to learn about rhythm is to try and compose a drum beat.  If you''ve ever seen a drum kit, you''ll know that it contains lots of different drums.  Each of these can play a different rhythm pattern to add to the music. \r\n\r\nIn this lesson we have a look at some simple drum beats.  Hopefully you''ll feel confident that you could write a drum beat of your own!', 'rhythm, composition, drum beats, drum kit', 'building-a-drumbeat', 'wn3KNsWRY6E', '0000-00-00 00:00:00'),
('l', 104, 7, 5, 25, 'Note Length Names (Whole, Half, Quarter)', 'Now we''ve had a go at composing some rhythms, it would be good if we could share these with other people.  Musicians have special words they use to describe notes of different lengths.  All of these relate to how long in beats a particular sound should be. \r\n\r\nIn this lesson we learn about whole notes (which last for four beats), half notes (which last for two beats and quarter notes (which only last for one beat). ', 'rhythm, composition, whole notes, half notes, quarter notes', 'note-lengths-1', 'jUK7eB7w-2I', '0000-00-00 00:00:00'),
('l', 105, 7, 5, 26, 'Note Length Names (Eighth and Sixteenth)', 'Some more note lengths to learn here with this lesson covering eighth notes (which last for just half and beat) and sixteenth notes (which last for a quarter of a beat).\r\n\r\nThese notes are called eighth and sixteenth notes because we can fit 8 eighth notes into one whole note and 16 sixteenth notes into one whole note.\r\n\r\nIt is these shorter notes that often help a piece of music to have a groove and make us want to dance.  ', 'rhythm, eighth notes, sixteenth notes', 'note-lengths-2', 'MObtxuZHK3c', '0000-00-00 00:00:00'),
('l', 106, 7, 5, 28, 'Introducing Meter', 'Meter is a term used by musicians to describe larger groups that repeat throughout a piece of music.  \r\n\r\nYou might have sometimes tried to count along with a piece of music, for a lot of music you''ll be able to this by counting 1, 2, 3 and 4 over and over again.  \r\n\r\nThe reason this works is that the music is built on repeating patterns of four beats.  When this is the case the music can be said to have a meter of 4.  In this video we look at some other examples of music with a meter of 2 (meaning the music is split into two-beat groups) and 3 (meaning the music is divided up into three-beat groups). ', 'rhythm, meter, in 4, 4/4 time, 3/4 time, 2/4 time', 'introducing-meter', 'yHUyFI5Tcmw', '2012-05-02 20:09:31'),
('l', 107, 7, 3, 30, 'Introducing Melody', 'Melody is such a central element of almost all music.  I probably don''t even need to explain what a melody is, you probably already know. \r\n\r\nA melody is a series of notes brought together in a particular rhythm to produce a memorable tune. \r\n\r\nThe melody is perhaps the most important element of any piece of music and so composers spend a lot of time thinking up the best melodies they can.', 'melody, tune, air, composition', 'introducing-melody', 'WESQHuwOizM', '2012-05-02 20:09:46'),
('l', 108, 7, 7, 31, 'What is Harmony?', 'Harmony happens in music when two or more pitches are combined.  Combining pitches together can add sophistication and interest to music and there are almost infinite possible combinations to explore!\r\n\r\nWhen two notes are brought together to produce harmony, this is called a chord.  Learning about all of the different types and flavours of chord is really important in becoming a good musician.  \r\n\r\nWith a good understanding of harmony we can make a good melody even more effective!', 'harmony, chords, what is harmony', 'what-is-harmony', '-OX1FyfZHWM', '2012-05-02 20:10:16'),
('l', 109, 7, 7, 33, 'Major Triads', 'Some of the most basic chords that are used all the time in music are called triads.  As the name suggests, triads are chords made up of three different pitches.  \r\n\r\nWe have several different flavours of triad.  In this lesson we take a look at the major triad, which is very closely related to the major scale.', 'major triad, harmony, chords', 'major-triads', '81Zbg9wyuZ0', '2012-05-02 20:10:29'),
('l', 110, 7, 7, 34, 'Minor Triads', 'Another commonly used triad in almost all western music is the minor triad.  It is based on the minor scale and has a similarly darker colour to its major counterpart.  \r\n\r\nYou can build a minor triad if you know the first, third and fifth degrees of a minor scale.  Add these three pitches together and you''ve got your triad.', 'minor triad, minor scale', 'minor-triads', 'zuL9fkRGV3g', '2012-05-02 20:10:43'),
('l', 113, 7, 10, 47, 'Introducing Musical Form', '', '', 'introducing-form', 'V8JK-iXTuRg', '2012-05-06 20:05:30'),
('l', 112, 7, 7, 38, 'Building Chords from Triads', 'We''ve got a good understanding of triads now, but of course harmony would be pretty boring if all we ever did was use these triads in only their basic form.  \r\n\r\nIn this lesson we have a look at how we can turn basic triads into fuller chords by either spreading their notes out over different octaves, doubling up the notes of a triad or a combination of the two!\r\n\r\nThere are lots of different possible ways to turn a basic triad into a fuller chord.  Your job is to find the ones you like! ', 'chords, harmony, triads, voicings', 'building-chords-from-triads', 'mLnkQ_fMh28', '2012-05-04 11:13:31'),
('l', 114, 7, 7, 37, 'The Primary Triads', 'Have you met someone with the magical ability to play along with any song or tune they like?\r\n\r\nThese people have probably not done a deal with the devil, but have instead learned about the primary triads.\r\n\r\nAs long as you know the three primary triads belonging to a particular scale or key, you can successfully harmonize any melody based on that scale/key. \r\n\r\nFor both major and minor scales/keys the primary triads are those build on the first, fourth and fifth scale degrees of that scale.', 'primary triads, tonic, subdominant, dominant', 'primary-triads', 'tpD_QUElZUQ', '2012-05-07 20:44:39'),
('l', 115, 7, 12, 55, 'What is Timbre?', '', '', 'timbre-intro', '7k5Ml7HoVoQ', '2012-05-23 20:23:53'),
('l', 116, 7, 13, 64, 'Introducing Texture', '', '', 'introducing-texture', 'Ak4qezdv4ZU', '2012-06-29 15:22:04'),
('l', 117, 7, 16, 65, 'Monophonic Texture', '', '', 'monophonic-texture', 'hdyg7nYiMJM', '2012-06-29 15:23:36'),
('l', 118, 7, 16, 66, 'Homophonic Texture', '', '', 'homophonic-texture', 'xpscshv3Exk', '2012-06-29 15:25:02'),
('l', 119, 7, 16, 67, 'Polyphonic Texture', '', '', 'polyphonic-texture', '3jGkGvTJK7A', '2012-06-29 15:28:01'),
('e', 120, 7, 3, 4, 'Listening to Pitches - (Easy)', '', '', 'pitch-1', 'pitch-1', '2012-08-20 21:40:13'),
('e', 121, 7, 3, 5, 'Listening to Pitches - (Medium)', '', '', 'pitch-2', 'pitch-2', '2012-08-20 21:41:12'),
('e', 122, 7, 3, 6, 'Listening to Pitches - (Hard)', '', '', 'pitch-3', 'pitch-3', '2012-08-20 21:41:12'),
('e', 123, 7, 3, 7, 'Can you spot an Octave?', '', '', 'identifying-octaves', 'identifying-octaves', '2012-09-26 11:23:39'),
('l', 124, 7, 7, 35, 'Diminished Triads', 'Not as common as the major or minor triads, but diminished triads can be used both as an interesting special effect or a way of connecting a series of triads. \r\n\r\nYou can build a diminished triad by stacking two minor thirds.  \r\n\r\nAlternatively just choose a note, count up three notes, play this note, count up three notes and play that note - then you''ve got your diminished triad!', 'diminished triad, diminished chord, minor thirds', 'diminished-triads', 'e2r3ZB06mFY', '2012-11-01 10:58:41'),
('l', 125, 7, 7, 36, 'Augmented Triads', 'The last basic triad in music that we need to learn about is the augmented triad.  Again, this is not particularly common but allow us to create some interesting harmonies. \r\n\r\nAn augmented triad can be created by stacking two major third intervals.\r\n\r\nIt can also be created by choosing a note on the keyboard, counting up four notes, playing the note you land on, counting up four more notes and adding this note to the chord.  Then you''ve made you augmented triad!', 'augmented harmony, augmented triads', 'augmented-triads', 'LHNPJs3-1Bw', '2012-11-01 11:51:33'),
('l', 126, 7, 11, 52, 'What is Articulation?', '', '', 'articulation', 'O4Q_m7m9B58', '2012-11-05 18:30:26'),
('l', 127, 7, 11, 53, 'Articulation: Staccato', '', '', 'staccato', 'AaKTBKRNvoE', '2012-12-07 18:38:07'),
('l', 128, 7, 11, 54, 'Articulation: Tenuto', '', '', 'tenuto', 'nxfBzxiyG3c', '2012-12-07 18:38:54'),
('l', 129, 7, 5, 29, 'Triplets and Jazz Eighths', 'In this lesson we look at a triplet.  A triplet is when we split a beat up into three equal pieces - remember that an Eighth note is half beat and a Sixteenth note is a quarter of a beat.  A triplet lasts for a third of a beat. \r\n\r\nTriplets can help give music a laid back a groovy rhythm.  This might be why they are used a lot in jazz music.', 'triplets, jazz eighths, triplet eighth notes', 'triplets', 'A0h2WqMWsL4', '2013-02-16 14:23:12'),
('l', 130, 7, 14, 68, 'Introducing Intervals', '', '', 'introducing-intervals', '1UIlqlwsa_I', '2013-03-17 17:23:19'),
('l', 131, 7, 14, 70, 'Perfect, Diminished and Augmented Fifths', '', '', 'perfect-diminished-augmented-fifths', '5lMRQ_oDWGc', '2013-03-18 14:01:31'),
('l', 132, 7, 14, 69, 'Naming Intervals', '', '', 'naming-intervals', 'xrZKoEAJENU', '2013-03-18 14:30:40'),
('l', 133, 7, 14, 71, 'Perfect, Diminished and Augmented Fourths', '', '', 'perfect-diminished-augmented-fourths', 'LIl_z9O_2jg', '2013-03-28 21:41:46'),
('l', 134, 7, 14, 72, 'Major and Minor Thirds', '', '', 'major-and-minor-thirds', 'g5TFXRiLcoA', '2013-03-29 09:47:48'),
('l', 135, 7, 14, 73, 'Major and Minor Seconds', '', '', 'major-and-minor-seconds', '4iJ6M0eNEo4', '2013-03-29 12:37:19'),
('l', 141, 7, 14, 75, 'Major and Minor Sevenths', '', '', 'major-and-minor-sevenths', 'im4jy_VcGII', '2013-04-08 15:32:13'),
('l', 140, 7, 14, 74, 'Major and Minor Sixths', '', '', 'major-and-minor-sixths', 'gm_cYKAzLH8', '2013-04-08 14:30:35'),
('l', 142, 7, 14, 76, 'Interval Recognition Practice', '', '', 'interval-recognition-practice', 'wmWafzb6E6o', '2013-04-08 18:47:07'),
('l', 143, 7, 14, 77, 'Compound Intervals', '', '', 'compound-intervals', 'QOi5nzl_a6M', '2013-04-09 19:05:20'),
('l', 144, 7, 14, 78, 'Major and Minor Ninths', '', '', 'major-and-minor-ninths', 'eNsDSah2La4', '2013-04-11 21:00:30'),
('l', 145, 7, 14, 79, 'Major and Minor Tenths', '', '', 'major-and-minor-tenths', 'eUwUa62jX2Q', '2013-04-15 10:58:26'),
('l', 146, 7, 9, 39, 'What is a mode?', 'You might have heard someone describe a piece as being ''modal'' or belonging to a particular mode.  So, what does this mean?\r\n\r\nThe modes are a set of scales that are some of the oldest in existence.  \r\n\r\nIn their most basic form, they are the scales that we get when we build scales out of just the white notes on the piano.  \r\n\r\nEach of these modes has a slightly different flavour and characters and so in the next few videos, we''re going to take a look at each one in turn.', 'modes, introduction to modes, gregorian chant, phrygian mode, lydian mode', 'what-is-a-mode', '38QfTP_LsAc', '2013-04-16 19:35:05'),
('l', 147, 7, 14, 80, 'Elevenths and Eleventh Chords', '', '', 'elevenths-and-eleventh-chords', 'NbSxkc-WIcA', '2013-04-18 21:17:16'),
('l', 148, 7, 9, 40, 'The Dorian Mode', 'The dorian mode, in its most basic form, is the scale that we get when we play all the piano white notes between D and D.\r\n\r\nIt has a cool character and has been used by jazz musicians like Miles Davis and Bill Evans to excellent effect.  \r\n\r\nThe dorian mode differs from the natural minor scale in only one way, the sixth degree of the scale is raised one semitone, which gives the scale its particular character. ', 'dorian mode, cool jazz, modes, Bill Evans, Miles Davis', 'dorian-mode', 'n_TuN-EIg3w', '2013-05-04 16:18:51'),
('l', 152, 7, 9, 41, 'The Phrygian Mode', 'The phrygian mode conjures up images of the Mediterranean or Middle East and can be built, in its most basic form, using the white notes between E and E.\r\n\r\nThe semitone step between the first and second degrees of the scale is what gives the phrygian mode its exotic character.  No major or minor scale starts this way and so right away when we hear this scale we know we are dealing with something unusual!', 'modes, phrygian mode, Spain, Middle East', 'phrygian-mode', 'MaZLwBej49M', '2013-05-12 20:26:58'),
('l', 150, 7, 14, 81, 'Perfect, Diminished and Augmented Twelfths', '', '', 'perfect-diminished-augmented-twelfths', 'EW7vmjBBjsM', '2013-05-11 16:36:34'),
('l', 153, 7, 9, 42, 'The Lydian Mode', 'The lydian mode, in its most basic form is created by playing the white notes between F and F.  \r\n\r\nIts most interesting feature is that it differs from the major scale by having a sharpened fourth scale degree - in the case of F lydian, the fourth note of the scale is a B natural (in F major, this would normally be a B flat).\r\n\r\nThe presence of this sharpened fourth in the mode produces melodies that sound altered, distorted and otherworldly. \r\n\r\nWe finish by having a look at the melody and harmony of the theme tune to ''The Simpsons'' - a piece of music based on the lydian mode.', 'lydian mode, augmented fourth, the simpsons theme music. ', 'lydian-mode', 'q4TKsBycJ6o', '2013-05-19 10:38:47'),
('l', 154, 7, 14, 82, 'Thirteenth Intervals and Thirteenth Chords', 'Perhaps the largest interval that we often talk about in music is the thirteenth.  A thirteenth is a compound sixth and so can be produced by adding together the interval of an octave and the interval of a sixth, be that a major sixth or a minor sixth. \r\n\r\nA leap of a thirteenth in a melody is rare and it would be awkward to ask vocalists and some instrumentalists to sing or play one.  However, a thirteenth is a very useful and colourful ingredient to add in to a chord.  You can have a listen to some thirteenth chords in this lesson!', 'intervals, thirteenth interval, thirteenth chords', 'thirteenths-and-thirteenth-chords', 'D96R5TbdcQA', '2013-05-20 11:37:28'),
('l', 155, 7, 9, 43, 'The Mixolydian Mode', 'We''ve made it as far as learning about the mixolydian mode, which in its basic form is the white note scale from G to G.\r\n\r\nThis mode can be found on lots of pop, rock and blues music as it includes a very cool flattened seventh at the top of the scale, compared with its major equivalent.  \r\n\r\nYou probably already know a piece of music that uses the mixolydian mode - "Hey Jude" by the Beatles.  Right at the end of the song Paul McCartney uses the mixolydian mode to create a blues chorus that we all love to sing along with. ', 'modes, mixolydian mode, the beatles, hey jude, paul mccartney', 'mixolydian-mode', '8dKVMZ3gd-w', '2013-05-25 15:34:21'),
('l', 156, 7, 9, 44, 'The Aeolian Mode (and the tierce de picardie!)', 'If you''ve follow all the videos up to this point - then you''ve already met the aeolian mode.  It is just another name for the natural minor scale. \r\n\r\nYou can build an aeolian scale by playing all the white notes on the piano between A and A.  \r\n\r\nLots of composers have used the aeolian mode to create both melodies and harmonies. \r\n\r\nAt the end of the video I improvise a piece based on the aeolian mode and also make use of a tierce de picardie.  This is a technique we can use to make a piece in a minor key more interesting.  We do this by finishing the music on a major chord rather than and minor chord, which we would be expecting.', 'aeolian mode, modes, natural minor scale, improvisation, tierce de picardie ', 'aeolian-mode', 'JYCdFHmvVTA', '2013-05-26 11:03:12'),
('l', 157, 7, 9, 45, 'The Locrian Mode', 'We complete our exploration of modes with the locrian mode.  Perhaps the least used of all the modes, you can build a locrian mode with the white notes running between B and B.\r\n\r\nOne of the reasons that this locrian mode is less commonly used is that it has an interval of a diminished fifth between its first and fifth scale degrees.  This is different to every other major, minor or modal scale that we''ve looked at.  \r\n\r\nComposers have struggled to write strong melodies and harmonies using this mode. \r\n\r\n', 'modes, locrian mode, diminished fifth, modal', 'locrian-mode', 'XJBAASlipMQ', '2013-05-27 09:40:37'),
('l', 159, 7, 9, 51, 'Relative Modes', 'Now we have a good understanding of what modes are and how we can build them, we''ve now going to look at ways in which we can group them together. \r\n\r\nRelative modes are groups of modes composed from the same set of pitches.', 'modes, relative modes, improvisation, composition', 'relative-modes', 'B6gGYCkU6nw', '2013-06-10 11:57:52'),
('l', 161, 7, 9, 52, 'Parallel Modes', 'Another way we can collect modes together is by grouping them by starting note.  These are parallel modes, modes that share their starting pitch.', 'modes, parallel modes, pitch', 'parallel-modes', 'AADYbu_s25E', '2013-06-10 13:42:28'),
('l', 162, 7, 8, 39, 'Introducing the Cycle of Fifths', 'The cycle (or circle) of fifths will blow your mind!  It is a way of representing the 12 pitches in a way that shows lots of the key ideas and relationships of Western music. ', 'cycle of fifths, circle of fifths, diatonicism, fifth relationship', 'cycle-of-fifths-introduction', 'SmoTrzmDktc', '2013-06-10 21:34:11'),
('l', 163, 7, 8, 40, 'Cool Things about the Cycle of Fifths: Scale Relationships', 'There are lots of cool things about the cycle or circle of fifths.  In this lesson, we look at the how the cycle sets out for us the relationships between the 12 major keys and scales.', 'circle of fifths, cycle of fifths, scales, keys, sharp keys, flat keys', 'cycle-of-fifths-scale-relationships', 'Kyy8miWMRtY', '2013-06-16 21:53:15'),
('l', 165, 7, 8, 41, 'Cool Things about the Cycle of Fifths: Minor Scales', 'We''ve looked at the major scales in the cycle of fifths, but what about the minor scales?  In this video, we look at how just the same the minor scales can be made into a cycle of fifths too, and can be connected up to their relative majors.', 'minor scales, cycle of fifths, circle of fifths, relative minors', 'cycle-of-fifths-minor-scales', 'Q69wcnHRJ2o', '2013-06-17 14:26:44'),
('l', 166, 7, 8, 42, 'Cool Things about the Cycle of Fifths: Harmony', 'Need to know which chords are your primary and secondary triads in a particular key? It''s right there in the cycle of fifths!  Guaranteed to amaze. ', 'cycle of fifths, circle of fifths, harmony, primary triads, secondary triads', 'cycle-of-fifths-harmony', 'av6PwZ78zKY', '2013-06-26 21:20:26');

-- --------------------------------------------------------

--
-- Table structure for table `PlaylistItem`
--

CREATE TABLE `PlaylistItem` (
  `id` int(11) NOT NULL,
  `title` varchar(75) NOT NULL,
  `text` varchar(5000) NOT NULL,
  `relid` int(10) NOT NULL,
  `youtubeid` varchar(20) NOT NULL,
  `credit` varchar(50) NOT NULL,
  `sort` tinyint(4) NOT NULL
) ENGINE=MyISAM AUTO_INCREMENT=85 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `PlaylistItem`
--

INSERT INTO `PlaylistItem` (`id`, `title`, `text`, `relid`, `youtubeid`, `credit`, `sort`) VALUES
(2, 'Taiko Drums', 'Now these are some serious drums!  Taiko drums originate in Japan where they were used by the Japanese army to help set a marching pace for the soldiers.  Taiko ensembles like this have only been around for about fifty years or so, but they can put on some incredibly impressive displays of both physical agility and rhythmic precision. ', 11, 'fWg6OLqHlNM', 'kedarvideo', 1),
(3, 'Jazz Vibes', 'This video features Gary Burton, one of the world''s leading vibraphone players.  With the help of four mallets Gary is able to produce shimmering melodies and harmonies on his instrument.  The music is <i>Libertango</i> by Argentinian composer Astor Piazzolla.', 11, 'Kxtr86zmYbk', 'mrmaxairistas', 1),
(4, 'Dennis Solo', 'This is Dennis Chambers, one of my favourite jazz drummers.  In this solo, Dennis keeps things interesting combining complex rhythms with the full range of sounds on his kit.  You''ll hear sections in this piece where there is a clear beat, and others where the beat is much freer.  Controlling the groove in this way is central to jazz drumming style. ', 11, 'YdwCf2xIxOo', 'pihdrummer', 1),
(5, 'Vibrating String', 'In these sections, I''ll be producing a playlist of videos that will illustrate the points I make in each lesson.  One of the fantastic things about exploring music, is that we need to do lots of listening, something you probably already do for fun!  Here we see vibrations passing through a string, they are much easier to see in slow motion.  These vibrations eventually reach our ears via the body of the instrument and the air around us.  ', 41, 'oyLfCPNf_hE', 'igiwarcraft', 1),
(6, 'Vibrating Cymbal', 'While we can see a guitar string vibrating, vibrations in other instruments are not so clear until we really slow them down.  This is a cymbal being hit in super slow motion, you can see the waves of energy passing through the metal as the cymbal bends out of shape.', 41, 'tBZ8o0Kiz6w', 'thissiteishorrible', 2),
(7, 'Not Just Nice Tunes', 'If you asked them, a lot of people might say that music was about producing things that are "good to listen to". But really, the definition of music is a lot wider than this.  I''ve talked about music being "Humanly Organised Sound", so if we take any sounds and choose to arrange them, then this becomes music.  This piece is arranges sounds in unconventional ways, was written by the Hungarian composer György Ligeti.  This is possibly not the piece to listen to after a hard day at work or school!', 41, 'qj9QlWltv8s', 'abbjorko', 3),
(8, 'Do you get where I''m coming from?', 'Which sounds composers choose to organise into music tells you a huge amount about who they are, when they were alive and where they lived.  Many songwriters tell stories about the places they come from in their music, like this song by New York rapper Jay Z.', 41, '0UjsXo9l6I8', 'jayz', 4),
(9, 'A World to Explore', 'There is music in every society in the world.  In some cultures, music is used to mark the changing of morning to afternoon and of summer to winter.  This piece of music from Japan is called "Sakura" and depicts the cherry blossoms that appear there every spring.', 41, 'N-dzfI3L5ic', 'michiganwinters', 5),
(10, 'Metronome', 'This is a metronome.  They are helpful as we can use them to show us what a regular beat at a particular speed sounds like.  Musicians often practice along to the ticking of a metronome to make sure they are not speeding the music up, or slowing it down accidentally.  ', 38, 'Oop2kBOANW4', 'cloverinesalve', 1),
(11, 'Spiegel Im Spiegel', 'This is a piece called <i>Spiegel Im Spiegel</i> by the Estonian composer Arvo Pärt.  It is incredibly simple and relaxing music.  The pianist''s right hand acts almost like a metronome, marking out the beat upon which the viola player can play long sustained notes. ', 38, 'TJ6Mzvh3XCc', 'playingmusiconmars', 1),
(12, 'Four to the Floor', 'A large amount of dance music relies on a heavy beat to get people moving their feet.  This is a vocal arrangement of a well known dance track.  For the majority of the song, there is a constant thumping bass drum beat.  You can see how different the music sounds when this beat drops away at around 0:52. ', 38, 'UtBeobpTcmk', 'pbpproductions', 1),
(13, 'Hiding the Beat', 'Sometimes music tries its best to hide the beat.  It is very slow and not very obvious but even this very free playing style does have some kind of underlying beat, moving the music forward.  In Eastern European music like this, slow, free playing is often followed by quick dances which are played to a very definite beat.', 38, 'OYzbyne58pk', 'tmjexpress', 1),
(14, 'Clap to the Beat', 'A very natural reaction when we hear a piece of music is to want to clap along to the music''s beat.  In classical music this is normally not encouraged, but in this New Year''s Day concert, the orchestra made an exception!', 38, 'Tz34Pdi59_A', 'karajanviolin', 1),
(15, 'Left, Right', 'Rhythm does a lot to give music its character.  This is a march played by the Coldstream Guards.  The notes are arranged into regular and ordered rhythms, designed to keep an army marching in step.  You should be able to count off "1, 2" or say "Left, Right" in time with the music.  ', 39, 'eAe6Mh6zbqY', 'themarches09', 1),
(16, 'Feeling the Groove', 'Now try counting "1, 2" or saying "Left, Right" to this piece!   In this groovy jazz number, the beats are arranged in to groups of five, which is why this piece is known as "Take Five".  In addition, the guitar player, George Benson, is playing rhythms that go against the underlying beat creating a groovy syncopated effect.  ', 39, 'Tn27IcAapPI', 'jazzaudrey', 1),
(17, 'Soprano Sax', 'There are far too many pitches for most instruments to play.  Instead, we often see families of instruments that cover the range of pitches composers generally want to write in their music.  Here is a soprano sax, which can play floating high pitches.', 42, 'SPRy3kG8jKs', 'crazydaisydoo', 1),
(18, 'Alto Saxophone', 'This is the alto saxophone, it overlaps with the soprano sax, but can also play lower notes as well.', 42, 'MmexBYQWh9U', 'bopppper', 1),
(19, 'Tenor Saxophone', 'Lower still is the tenor saxophone.  The tone of the instrument is richer, which suits the lower pitches that this instrument can play. ', 42, 'f1CFv5LB9HA', 'bopppper', 1),
(20, 'Baritone Sax', 'This is the baritone saxophone.  It is often used to play basslines and takes care of the lower notes composers might want to write.  There are even larger, lower sounding saxophones than this, but we don''t seem them used very often.  Although, go and look up the contrabass saxophone, if you''d like a laugh!', 42, 'I6sRAHpBagc', 'jazzchess', 1),
(21, 'On go on then . . . ', 'Here it is. ', 42, 'x1t_vu_uQoQ', 'saxtek', 1),
(22, 'Organ', 'There''s no sound more impressive than an organ turned up to eleven!  With so many sets of pipes to choose from the player can make a range of different sounds on the instrument, and can combine sounds to produce earth-shaking effects.  This piece is the Toccata from Widor''s Fifth Organ Symphony, played here by Frederick Hohman in the Cathedral Basilica of the Sacred Heart in Newark, New Jersey.', 20, 'DKejfYzB3ak', 'midnightpipes', 1),
(23, 'Harpsichord', 'It is great to hear Bach''s music played on an instrument for which it was written.  Using the upper and lower keyboards, the player is able to produce louder and softer sounds, however, generally the music is all approximately the same volume level.  Music played on the harpsichord sounds crisp and clear and it really suits the intricate polyphony that composers were writing in this period.  ', 20, 'yOP7z54BXYo', 'ecomparone', 1),
(24, 'Piano', 'Here is the same piece, but this time played on a modern grand piano.  You will hear that the instrument sounds very different to the harpsichord, and in addition, the player is able to vary how loud and soft he is playing by touch alone.  This is a great performance as this player''s light and clear touch on the instrument reminds us of the harpsichord.  ', 20, 'fVUcPpMpuRg', 'fuzzy8balls', 1),
(25, 'Rachmaninov', 'This is a piece by Russian composer Sergei Rachmaninov, written at the start of the Twentieth Century.  The music shows of the full range of pitches the piano can produce, and sets detached short playing along side some beautiful singing passages.', 20, 'acVODwPb1G8', '84036980', 1),
(26, 'Bill Evans', 'The piano hasn''t just been used in classical music.  Jazz musicians have often been drawn to the instrument as it allows them to explore complex harmonies and melodies on their own.  This is a performance by Bill Evans, well known for playing with Miles Davis on the album "Kind of Blue".', 20, 'mRhVI7cpcS4', 'drthimarques', 1),
(27, 'Violin', 'An archive recording of virtuoso (master) violinist Jascha Heifetz.  Without the aid of frets to help them, violin players spend years learning exactly where on their fingerboard they need to press to produce the pitches they need.   ', 15, 'tAVXJQDXItI', 'sim55', 1),
(28, 'Viola', 'You''ll hear the viola is able to produce a much warmer, richer tone than the violin.  It is more suited to melodies that are slow and graceful not fast and energetic.', 15, '17lvqMdHyxQ', 'fanzowitz', 1),
(29, 'Cello ', 'This is the very well-known theme "The Swan" from Carnival of the Animals by Saint Seans.  Played by Julian Lloyd Webber.  ', 15, 'Moh9KxC_iQA', 'cheapskate1p', 1),
(30, 'Double Bass', 'Most of the time the double bass sits in the shadows playing basslines while other instruments grab the limelight.  However, every now and again, the double bass is used as a solo instrument, like in this example by Miloslav Gajdos. ', 15, 'Tg4kr9tDtjI', 'jcobalis', 1),
(31, 'Electric Guitar', 'In the right hands the electric guitar can be made to wail, shout and sing.  Chuck Berry''s guitar playing on the song "Johnny B Goode" has made it a rock and roll classic.', 15, 'AEq62iQo0eU', 'helminen1', 1),
(32, 'Flute', 'Sounding far better than me blowing across the top of a bottle, here is Bach''s Minuet and Badinerie from his Suite in B Minor.  See if you can spot the flute soloist swapping melodic ideas with the rest of the orchestra.', 18, '4sAh02JRtpk', 'bencio91', 1),
(33, 'Oboe', 'Virtuoso Oboist Nicholas Daniel plays a movement from Benjamin Britten''s Six Metamorphoses after Ovid.  In solos like this one you can really hear the instrument''s expressive qualities. ', 18, 'TMzrVYM6Bpo', 'nicholasdanieloboe', 1),
(34, 'Benny Goodman', 'Few players are as easy to identify as clarinettist Benny Goodman - performing here with his trio.  ', 18, 'O_h-saU6gNA ', 'zpants', 1),
(35, 'Bassoon', 'Another instrument that we don''t hear on its own very often is the bassoon, which I think is a great shame.  It is capable of some very elegant melodic playing.  ', 18, 'vhyVvH6FPYI', 'soyupsy', 1),
(36, 'Miles Davis', 'Producing some of the most important jazz albums of the twentieth century, Miles Davis introduced whole new ways of playing the trumpet, unheard before this point.  ', 18, 'j7lkqxfvA78', 'cralik', 1),
(37, 'Trombone', 'Another very versatile instrument is the trombone.  Using the slide allows the player great expressive control of the sound, making very subtle changes to note pitches.', 18, '9U_Gk-5vPWU', 'martywilson1962', 1),
(38, 'Brass Line Out', 'One thing that brass instruments can do is play loud!  And so when you have this many together in one place, you''re going to be in for an impressive show.  Bands like this play in support of college sports teams in the United States and lead the crowd in singing. ', 18, 'ZV31SToh_MM', 'r0nej00', 1),
(39, 'Duet', 'Virtuoso Pianist Lang Lang and his father play a piece of traditional Chinese music called "Horse".  While Lang Lang is famous for being a piano soloist and playing concertos, he is playing the role accompanist in this performance.  He is listening very carefully to the sound of the Erhu to make sure that he isn''t playing too loud so we can''t hear it and allowing his father to play expressively.  You''ll see that the Erhu is capable of producing a wide range of sounds - even a few horse noises towards the end! ', 22, 'Lji834exsgg', 'rupertjones', 1),
(40, 'Trio', 'Here is the first movement of a piano trio written by the French composer Maurice Ravel.  The combination of piano, violin and cello is a very popular choice due the wide range of sounds and textures these instruments can produce.  You''ll notice here that the piano is not just left on the sidelines accompanying the solo instruments, but is also given moments to shine with its own melodies and solo passages. ', 22, 'dmjdKhn3YgM', 'hppiano1', 1),
(41, 'String Quartet', 'Here is the opening movement of Shostakovich''s Eighth String Quartet, it is probably the most famous he wrote.  It was composed on a trip to Dresden, Germany and might well have been inspired by the war-torn city the composer would have found.  The string quartet is very much a team effort with each player having important moments to play in the music.     ', 22, 'gSoKpCXWF0Q', 'opusoctopus', 1),
(42, 'Quintet', '. . . and add one more makes five!  Quintets are also popular with composers as it allows to blend instruments from different families together.  In the Trout Quintet, for example, Schubert adds a piano to broaden the range and power of the string quartet.', 22, 'kd8_fLqz3gk', 'win081', 1),
(43, 'Mars', 'There are so many fantastic pieces of music for orchestra, but Gustav Holst''s Planets Suite is one of my favourites.  Holst writes one movement representing each planet (except for Earth and Pluto, which hadn''t been discovered when this piece was written).  In these pieces you can hear a huge range of the different sounds an orchestra can make.  This is Mars, the Roman God of War, and you can hear a lot of military music here, with the march rhythms in the strings and percussion and the fanfares from the very large brass section. ', 24, 'AGGlL1wexQk', 'rupertjones', 1),
(44, 'Venus', 'Venus is next on our trip through the planets (you may have noticed they do not come in the right order!)  I have a theory that it was this piece that inspired John Williams when he was writing the music to the film Star Wars.  Planets - Star Wars, it is quite easy to see the connection.  The haunting calm of the opening french horn solo is similar to that used by Williams for Han Solo''s theme.  If you''ve never seen Star Wars - go and see it!', 24, 'PyBkzZoMYN4', 'rupertjones', 1),
(45, 'Mercury', 'Mercury in Roman mythology was a winged messenger who would carry messages between the gods.  You can hear him flying about in the sky in the quick playing from the strings darting up and down at great speed', 24, 'CilfBWvCSXI', 'rupertjones', 1),
(46, 'Jupiter', 'The jolly giant, Jupiter is the largest planet and was the King of the Roman gods.  The central theme of this movement beginning at about 3:00 has become very well known through out the world.  ', 24, 'OYdzb6TZW7M', 'rupertjones', 1),
(47, 'Saturn', 'A very different character greets us as we reach Saturn on our trip through the solar system.   In Roman myths Saturn was a god associated with old age and death.  A very slow tempo and a plodding bassline give a very heavy and tired feeling to the music.   ', 24, 'R23xW7xpm-Y', 'rupertjones', 1),
(48, 'Uranus', 'With Uranus we are dealing with a mysterious and unpredictable character.  Changing mood very quickly the music shifts from fast string passages to long ominous brass chords. ', 24, 'lGfwxpuY2jY', 'rupertjones', 1),
(49, 'Neptune ', 'We''re well out into deep space by the time we reach Neptune.  Twinkling stars and planets can be heard in the distance as a mystical choir echos from some distant galaxy to complete the piece. ', 24, 'ZQQGi4gN6gI', 'rupertjones', 1),
(50, 'The Frontman', 'Being the frontman of a rock group is about far more than just singing, keeping the audience engaged, communicating the meaning of the songs and making sure everyone has a great time are all very important, too.  Queen frontman Freddie Mercury was a master at all of this.  Here he is entertaining thousands at the London Live Aid concert in 1985.', 26, 'lDckgX3oU_w', 'oberon1966', 1),
(51, 'The standard line up', 'The worldwide success of the Beatles in the 1960s led lots of other rock and pops to adopt their line-up.  With lead and rhythm guitarists, a bass player and a drummer.  All four members of the group also sang.', 26, 'pVlr4g5-r18', 'beatlescentral', 1),
(52, 'Playing together', 'The effect of a lead guitarist and bassist playing together is incredibly powerful and is used in a lot of heavier rock music.  Here is an example by the British rock group Led Zeppelin', 26, 'HQmmM_qwG4k', 'ledzeppelin', 1),
(53, 'The Power of Bass', 'Although in other styles of popular music the bassist plays a much more independent role.  This couldn''t be truer in funk music where the groovy rhythms of the bass player give the music its danceable feel.  Here the excellent bass playing from Flea of the Red Hot Chilli Peppers underpins the rest of the music. ', 26, '_hR3b6q4ubI', 'jhap1982', 1),
(54, 'Sonata - 1', 'So it is time to sit back, relax and listen to some music in the forms that we have just discussed.  This sonata was written by Mozart and is performed here by Chinese pianist Lang Lang.  This is Mozart''s Sonata in B flat Major K.333.  We would expect opening movements of sonatas to contain some exciting fast playing and this is certainly the case here with lively scale passages in the right hand.', 45, 'dbAF0tWX53Q', 'musicclassical1', 1),
(55, 'Sonata - 2', '. . . and so on to the second movement.  The music is much slower, graceful and sustained than the opening movement. ', 45, 'bh0jhUw7eaM', 'musicclassical1', 1),
(56, 'Sonata - 3', 'Returning to the fast lively music of the opening Mozart completes this sonata with a range of flourishes that show off the piano as a very versatile and expressive instrument.  So across this piece we have seen the fast - slow - fast arrangement of movements which is so common in music of this late 18th, early 19th century period.', 45, 'BNPyUz82JZU', 'musicclassical1', 1),
(57, 'Concerto - 1', 'This is the concerto that Dimitri Shostakovich wrote as a 19th birthday present to his son Maxim.  Just as in sonatas many concertos being with a fast opening movement.  Listen out for the balance between solo playing from the pianists, ensemble playing from the orchestra and section where the two come together.', 45, 'PUfw5coaVPI', 'kirillgerstein', 1),
(58, 'Concerto - 2 ', 'This beautiful slow movement is one of Shostakovich''s most famous works, showing how sometimes the best music can be incredibly simple.  The piano really leads the rest of the players in shaping the expression and movement in the music.', 45, 'OjPFSCW21j0', 'kirillgerstein', 1),
(59, 'Concerto - 3', 'Maxim Shostakovich must have been an excellent pianist as he performed this piece only a matter of days after his birthday.  You can hear the dance rhythms alongside dazzling virtuoso passages from the piano, and this piece has one of the most exciting endings I know!', 45, '6CKqobY7l84', 'kirillgerstein', 1),
(60, 'Symphony', 'Hope you''re sitting comfortably as we listen to a symphony by Ludwig van Beethoven, his seventh.  This is a long piece and it might be better to listen to it in small sections, rather than all the way through in one go.  There is lots I could say about the music, but I think it best for you to listen and discover it for yourself.  Try and see if you can spot as the music changes mood and character throughout the work.  Enjoy!', 45, '7MqrBauptrE', 'obtica1', 1),
(61, 'Barbershop', 'You can see why harmony has often been seen as music''s most magical component.  This style of singing is known as close harmony and is famed for its smooth and colourful use of harmony.  Blending four voices or instrument parts together is a central part of Western music. ', 47, 'Ie7oKkg7wYw', 'barbershopharmony38', 1),
(62, 'Campfire Guitar', 'Harmony doesn''t have to complex to be effective.  Many singers choose to provide their own harmony strumming away simple chords on the guitar. ', 47, 'a5amzHKp9x8', 'valw151', 1),
(63, 'Modern Harmony', 'Harmony occurs whenever a composer uses two different music sounds at the same time.  More modern music has attempted to move away from more standard chords to bringing sounds together in more unusual combinations. ', 47, 'KTGwFZxRRbU', 'musicxcomposers', 1),
(64, 'Worksong', 'While this worksong was recorded in the middle of the twentieth century, it gives us a good idea of the sounds we might have heard out in the plantation field.  Listen out for the call and response structure of the music. ', 34, '4G5KtQynWvc', 'abanks47', 1),
(65, 'Early Blues', 'This is blues guitarist and singer Blind Willie Johnson.  Keeping the call and response pattern from the worksong, these early blues players began to accompany themselves on other instruments, and most often the guitar. ', 34, '5hucTDV1Fvo', 'velotronic', 1),
(66, 'Jazz', 'While we know Louis Armstrong as the singer of popular songs like "What a Wonderful World", his trumpet playing technique and improvisation were very important during the development of Jazz.  The solo at the beginning of this piece "West End Blues" is almost like a cadenza, showing off his excellent technique and creative ideas.  ', 34, 'W232OsTAMo8', 'cretenick', 1),
(67, 'Bird', 'Charlie Parker (known as Bird) was one of the musicians responsible for pushing the limits of Jazz with increasingly complex harmonies and improvisation.  ', 34, 'S4mRaEzwTYo', 'minicqqper', 1),
(68, 'Rock & roll', 'During the 1940s and 1950s rock and roll music became incredibly popular with stars like Elvis Presley making use of television for the first time to get his music in to the living rooms of the United States.  Much simpler and to the point than the complex jazz of the time, young people found this music immensely exciting.  ', 34, 'Bm5HKlQ6nGM', 'yelserp22', 1),
(69, 'Rock ', 'Following on from the success of the rock and roll stars, record companies were always on the lookout for new artists and groups playing new styles.  British groups like the Rolling Stones, who combined the simplicity of rock and roll with the rough edge of the blues, produced some of the earliest examples of rock music.  The standard guitar, singer, drums and bass line up is still common in bands being formed today. ', 34, '8_VbImuG71M', 'imaStonesGirl1', 1),
(70, 'Baroque', 'This is a wonderful gem that comes to us from J. S. Bach, his concerto for two violins.  So how can we tell this piece comes from the Baroque period?  Firstly, we can see the harpsichord at the heart of the action.  More importantly though is the way the music is constructed, rather than one instrument that is the focus of attention, each part coming together to form the music.  This is the technique known as counterpoint and is a central feature of music from this period. ', 30, 'LIOh5Iq683o', 'OxD1Bn99CO', 1),
(71, 'Classical', 'Once we get to Mozart (1756-1791), music has clearly moved on from the Baroqie period.  Instead of the complex interweaving of melodies into counterpoint, most of the time in this music there is only one part which has the main melody, and the other instruments just play chords to accompany it.  You can hear this very clearly at the opening of this Divertimento, where the first violins play the melody, while the other strings play chords together.  ', 30, 'dPBL-kPQzkc', 'hamjii', 1),
(72, 'Early Romantic ', 'Attempting to broaden the scope and style of the music they were writing, composers in the Romantic period began to write pieces inspired by ideas, people, places and philosophies.  This piece known as "Fingal''s Cave" was written by Mendelssohn (1809-1847) after his trip to the Scottish Hebrides Islands.  It is like Mendelssohn is painting a picture for us of the dark and mysterious landscape of the Scottish Isles, with the sun occasionally breaking through in the lighter moments.  ', 30, 'TJHaIevlvac', 'thelightisahead', 1),
(73, 'Late Romantic ', 'As the nineteenth century continued, composers used bigger and bigger orchestras to power their ambitious ideas.  This piece by Richard Strauss attempts to tell the story of an attempt to climb an Alpine mountain.  At this point the climbers have reached the top as the music represents both the view they discover and the feeling of joy at having scaled the summit. ', 30, 'xK7z2NhUrsQ', 'berlinphil', 1),
(74, 'Atonality', 'As composers kept experimenting with music, some started to leave behind some of the established ideas about organising pitches and harmony that were established at the time.  The result was very different sounding music. ', 30, 'h0-4SZGCd_A', 'calkingjames', 1),
(75, 'Exploration', 'Another way composers could create new and exciting music was to start getting musicians to play their instruments in new or unusual ways.  This piece by American composer George Crumb also makes use of other objects to add other sounds to the music.  ', 30, 'm5a2RXA2Jn8', 'StanislavFilarmonica', 1),
(76, 'Electronics', 'The use of electronic instruments was not just resigned to rock and pop musicians.  Classical composers have also been very interested in the possibilities for new sounds this technology can provide.  This music is by the German composer Karlheinz Stockhausen (1928-2007).', 30, 'aNt6a5xFOnE', 'newmusicxx', 1),
(77, 'Spain', 'Dramatic music from Spain, Flamenco is played on its own, but also as an accompaniment for dance.', 36, '0Tsv2lTafEY', 'lacastorcita', 1),
(78, 'India', 'The history and heritage of India''s classical music tradition is one of the oldest in the world.  Rather than go to school, students often learn music from their family, inheriting the melodies and playing styles that have been passed down from generation to generation', 36, 'TxNmqOiWSqk', 'manavk', 1),
(79, 'Bali', 'Comprising a wide range of different instruments, the gamelan being played is a sight to behold.  Hearing one at the Paris World''s fair is said to have had a great effect on the composer Claude Debussy, who incorporated sounds like these into his music. ', 36, 'ldPMifPbngc', 'ferretguru', 1),
(80, 'Australia ', 'Music plays a very central role in the lives aboriginal living across the Australian outback.  Instruments like the didgeridoo are also much older than most of the instruments we have seen in Europe, with some of the oldest examples dating back 1500 years.  ', 36, 'dFGvNxBqYFI', 'johnxxx20000', 1),
(81, 'Eastern and Southern Africa', 'This is the sound of the Mbira, which can be found across many parts of the African continent.  The music from the Mbira plays a central role in tribal community and is used for celebration and dancing, but also in religious ceremonies. ', 36, 'tIPORpN27CY', 'ytclassic', 1),
(82, '12 Bar Blues', 'The twelve bar blues is a chord scheme that has been used in music ranging from rock and pop to soul and jazz.  Known for fusing together the sounds of the blues with motown and gospel music, Ray Charles often uses this chord scheme in his music.  See if you can identify the points in the music where we return to beginning of the 12 bar pattern.  Here''s a clue - this happens for the first couple of times at 0:25 and 0:46. ', 32, '_TgxQg3Z818', 'ovationtv', 1),
(83, 'Jazz ', 'Here is a masterclass in improvisation given to us by saxophonist Sonny Rollins.  Sonny starts by playing the calypso "St. Thomas" tune, but is improvising from 0:39.  Initially some features of the tune can be heard, but very quickly Sonny is composing brand new melodies.  Keeping all of this together is the chord scheme of "St. Thomas" which is repeated unchanged throughout the piece.  The members of the band also get a chance to try their skill at improvising, and then finally, at 9:51 the band returns to the "St. Thomas" tune to complete the piece. ', 32, 'v4DTR0I7xhA', 'bluesbebopper2000', 1),
(84, 'Pop Song', 'While the Beatles may have come from England their music was inspired by the Blues, Soul and Rock and Roll musicians of the United States.  The form of this song - "I Wanna Hold Your Hand", unfolds as follows.  After a short introduction, verse 1 begins at 0:07, followed by the chorus at 0:23.  This is repeated with verse 2 (0:30) and a second chorus (0:44).  It was common in songs like this to add a new section towards the middle of the song to stop the verse and chorus structure becoming boring. This is a called a middle 8 and that''s what happens next from 0:52.  Next comes a third verse and chorus.  Then the middle 8 again.  The song is completed by a final verse and chorus.    ', 32, 'MKHFUKZ-IXE', 'spartan117halofreak', 1);

-- --------------------------------------------------------

--
-- Table structure for table `SearchTerm`
--

CREATE TABLE `SearchTerm` (
  `id` int(11) NOT NULL,
  `term` varchar(300) NOT NULL,
  `frequency` int(11) NOT NULL DEFAULT '1'
) ENGINE=MyISAM AUTO_INCREMENT=146 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `SearchTerm`
--

INSERT INTO `SearchTerm` (`id`, `term`, `frequency`) VALUES
(1, 'Bob Marley', 1),
(2, 'Grade 5 theory', 1),
(3, 'C Major', 2),
(4, 'E Major', 1),
(5, 'aural', 2),
(6, 'harmonize', 4),
(7, 'theory', 1),
(8, 'workshop', 1),
(9, 'finishing phrase', 1),
(10, 'rythem', 1),
(11, 'circle of fifths', 2),
(12, 'scale degrees', 2),
(13, 'music curriculum early years', 1),
(14, 'meter', 2),
(15, 'tempo', 1),
(16, 'ionian', 1),
(17, 'guitar', 16),
(18, 'dissionance', 1),
(19, 'harmonizing', 1),
(20, 'harmonization', 1),
(21, 'harmony', 9),
(22, 'Articulation ', 1),
(23, 'clarinet', 1),
(24, 'german sixth', 1),
(25, 'rithm', 1),
(26, 'sibelius', 2),
(27, 'rudements', 1),
(28, 'Secondary dominant s ', 1),
(29, 'counterpoint', 4),
(30, 'electronic', 1),
(31, 'phrygian', 5),
(32, 'intervals', 9),
(33, 'cadences', 1),
(34, 'cadenza', 1),
(35, 'chord progression', 4),
(36, 'b minor', 1),
(37, 'd major', 1),
(38, 'major', 8),
(39, 'piano', 6),
(40, 'development', 1),
(41, 'dominant seventh', 1),
(42, 'four part chords', 1),
(43, 'guitar lessons', 1),
(44, 'chords', 9),
(45, 'triads', 2),
(46, 'music', 1),
(47, 'chord', 2),
(48, 'rhythm', 14),
(49, 'simple meter', 1),
(50, 'primary triads', 1),
(51, 'primary and secondary triads', 1),
(52, 'grade 5', 1),
(53, 'part writing', 1),
(54, 'progressive', 1),
(55, 'monteverdi', 2),
(56, 'Sing', 14),
(57, 'seventh', 2),
(58, 'suspended', 1),
(59, 'fourth', 1),
(60, 'Pitch', 1),
(61, 'basic music theory part 2', 1),
(62, 'basic music theory', 1),
(63, 'claves', 1),
(64, 'orchestration', 2),
(65, 'cadence', 1),
(66, 'voice', 1),
(67, 'vocal', 1),
(68, 'software', 1),
(69, 'composition', 2),
(70, 'inversion', 2),
(71, 'harmonic', 1),
(72, 'cirlce of fifths', 1),
(73, 'read music', 1),
(74, 'time signature', 5),
(75, 'modal', 1),
(76, 'Intelrval', 1),
(77, 'compose', 2),
(78, 'tensions', 1),
(79, 'Scales', 4),
(80, 'analysis', 1),
(81, 'chromatism', 1),
(82, 'notas musicais no piano', 2),
(83, 'como aprender a tocar piano', 1),
(84, 'arpeggio', 1),
(85, 'violin', 1),
(86, 'Coldplay', 1),
(87, 'politics', 1),
(88, 'diatonic', 1),
(89, 'semitone', 1),
(90, 'chromatic', 1),
(91, 'reggae', 1),
(92, 'partiture', 3),
(93, 'playing pino/organ ', 2),
(94, 'lessons on organ playing', 3),
(95, 'lessons on pino ', 1),
(96, 'instrument lessons', 1),
(97, 'part 8', 1),
(98, 'jazz', 2),
(99, 'arranging', 1),
(100, 'ear', 3),
(101, 'modes', 5),
(102, 'score', 1),
(103, 'sheet', 1),
(104, 'gutiar', 1),
(105, 'cadances', 1),
(106, 'drums', 1),
(107, 'tonality', 1),
(108, 'violão', 1),
(109, 'secondary triad', 1),
(110, 'chromatic harmony', 1),
(111, 'picardy', 1),
(112, 'tenuto', 1),
(113, 'what is music theory', 1),
(114, 'opera ', 1),
(115, 'sheet music', 2),
(116, 'how to play guitar', 1),
(117, 'trascription', 1),
(118, 'nirvana', 1),
(119, 'homophony', 1),
(120, 'tv news', 2),
(121, 'lessons', 2),
(122, 'vocabulary', 1),
(123, 'violoão', 1),
(124, 'compound time', 1),
(125, 'notation', 1),
(126, '30', 1),
(127, 'CGP Grey', 1),
(128, 'Debt limit', 1),
(129, 'structure', 1),
(130, 'contrapuntal devices', 1),
(131, 'melody', 1),
(132, 'secondary triads', 1),
(133, 'philnaldrett@google .com', 1),
(134, 'Texas', 1),
(135, 'Can Texas secede from the country', 1),
(136, 'the staff', 2),
(137, 'pedal', 1),
(138, 'perfect', 1),
(139, 'perfect intervals', 1),
(140, 'Major intervals', 1),
(141, 'major third', 1),
(142, 'interval', 2),
(143, 'Classic guitar', 1),
(144, 'percussion', 1),
(145, 'sonata', 1);

-- --------------------------------------------------------

--
-- Table structure for table `Topic`
--

CREATE TABLE `Topic` (
  `id` int(11) NOT NULL,
  `title` varchar(300) NOT NULL,
  `urltitle` varchar(30) NOT NULL,
  `colour` varchar(10) NOT NULL DEFAULT '#000000',
  `courseId` int(11) NOT NULL,
  `sortorder` int(11) NOT NULL
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Topic`
--

INSERT INTO `Topic` (`id`, `title`, `urltitle`, `colour`, `courseId`, `sortorder`) VALUES
(2, 'Getting Started', 'getting-started', '#000000', 7, 1),
(3, 'Pitch', 'pitch', '#000000', 7, 2),
(4, 'Scales', 'scales', '#000000', 7, 3),
(5, 'Rhythm', 'rhythm', '#000000', 7, 4),
(7, 'Harmony', 'harmony', '#000000', 7, 6),
(8, 'The Circle of Fifths', 'the-circle-of-fifths', '#000000', 7, 7),
(9, 'Modes', 'modes', '#000000', 7, 8),
(10, 'Form', 'form', '#000000', 7, 9),
(11, 'Articulation', 'articulation', '#000000', 7, 10),
(12, 'Timbre', 'timbre', '#000000', 7, 11),
(13, 'Instruments', 'instruments', '#000000', 7, 12),
(14, 'Intervals', 'intervals', '#000000', 7, 14),
(15, 'Ensembles', 'ensembles', '#000000', 7, 15),
(16, 'Texture', 'texture', '#000000', 7, 13),
(17, 'Music History', 'music-history', '#000000', 7, 16),
(18, 'Getting Started', 'getting-started', '#000000', 4, 1),
(19, 'Staff and Clefs', 'staff-and-clefs', '#000000', 4, 2),
(20, 'Reading Rhythm', 'reading-rhythm', '#000000', 4, 3),
(21, 'Measures/Bars', 'measures-bars', '#000000', 4, 4),
(22, 'Key Signatures', 'key-signatures', '#000000', 4, 5),
(23, 'Guitar Tablature', 'guitar-tablature', '#000000', 4, 9),
(24, 'Dynamics and Articulation', 'dynamics-and-articulation', '#000000', 4, 7),
(26, 'Tempo', 'tempo', '#000000', 4, 8),
(27, 'Sharps and Flats', 'sharps-and-flats', '#000000', 4, 6);

-- --------------------------------------------------------

--
-- Table structure for table `User`
--

CREATE TABLE `User` (
  `id` int(11) NOT NULL,
  `username` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(200) NOT NULL,
  `joinDate` int(11) NOT NULL,
  `lastActivity` int(11) DEFAULT NULL,
  `points` int(11) DEFAULT NULL,
  `l1badgeCount` int(11) DEFAULT NULL,
  `firstip` varchar(255) NOT NULL,
  `biog` varchar(255) NOT NULL,
  `subamount` int(11) NOT NULL
) ENGINE=MyISAM AUTO_INCREMENT=721 DEFAULT CHARSET=utf8;

--
-- Dumping data for table `User`
--

INSERT INTO `User` (`id`, `username`, `name`, `email`, `joinDate`, `lastActivity`, `points`, `l1badgeCount`, `firstip`, `biog`, `subamount`) VALUES
(720, 'ZruMczeEIffrGMBDjlXo', 'Cookie not set', 'noemailyet@tempuser.com', 0, NULL, 1, NULL, '::1', '', 0);

-- --------------------------------------------------------

--
-- Table structure for table `UserExerciseAnswer`
--

CREATE TABLE `UserExerciseAnswer` (
  `id` mediumint(8) unsigned NOT NULL,
  `userId` int(10) unsigned NOT NULL COMMENT 'CONSTRAINT FOREIGN KEY (userId) REFERENCES User(id)',
  `exerciseId` int(10) unsigned NOT NULL,
  `complete` tinyint(4) NOT NULL,
  `countHints` tinyint(4) NOT NULL,
  `timeTaken` smallint(5) unsigned DEFAULT NULL,
  `attemptNumber` mediumint(8) unsigned DEFAULT NULL,
  `timestamp` int(11) NOT NULL
) ENGINE=MyISAM AUTO_INCREMENT=20635 DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `UserVideoView`
--

CREATE TABLE `UserVideoView` (
  `id` int(10) unsigned NOT NULL,
  `userId` int(10) unsigned NOT NULL COMMENT 'CONSTRAINT FOREIGN KEY (userId) REFERENCES User(id)',
  `lessonId` int(10) unsigned NOT NULL COMMENT 'CONSTRAINT FOREIGN KEY (lessonId) REFERENCES Lesson(id)',
  `status` tinyint(4) NOT NULL,
  `position` varchar(100) DEFAULT NULL,
  `timestamp` int(11) NOT NULL
) ENGINE=MyISAM AUTO_INCREMENT=4184 DEFAULT CHARSET=latin1;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `Course`
--
ALTER TABLE `Course`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `Lesson`
--
ALTER TABLE `Lesson`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `PlaylistItem`
--
ALTER TABLE `PlaylistItem`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `SearchTerm`
--
ALTER TABLE `SearchTerm`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `Topic`
--
ALTER TABLE `Topic`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `User`
--
ALTER TABLE `User`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- Indexes for table `UserExerciseAnswer`
--
ALTER TABLE `UserExerciseAnswer`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `UserVideoView`
--
ALTER TABLE `UserVideoView`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `Course`
--
ALTER TABLE `Course`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=8;
--
-- AUTO_INCREMENT for table `Lesson`
--
ALTER TABLE `Lesson`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=167;
--
-- AUTO_INCREMENT for table `PlaylistItem`
--
ALTER TABLE `PlaylistItem`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=85;
--
-- AUTO_INCREMENT for table `SearchTerm`
--
ALTER TABLE `SearchTerm`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=146;
--
-- AUTO_INCREMENT for table `Topic`
--
ALTER TABLE `Topic`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=28;
--
-- AUTO_INCREMENT for table `User`
--
ALTER TABLE `User`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=721;
--
-- AUTO_INCREMENT for table `UserExerciseAnswer`
--
ALTER TABLE `UserExerciseAnswer`
  MODIFY `id` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=20635;
--
-- AUTO_INCREMENT for table `UserVideoView`
--
ALTER TABLE `UserVideoView`
  MODIFY `id` int(10) unsigned NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=4184;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `UserExerciseAnswer`
--
ALTER TABLE `UserExerciseAnswer`
;

--
-- Constraints for table `UserVideoView`
--
ALTER TABLE `UserVideoView`
;
