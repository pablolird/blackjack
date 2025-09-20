    package com.badlogic.blackjack;
    import com.badlogic.gdx.ApplicationListener;
    import com.badlogic.gdx.Gdx;
    import com.badlogic.gdx.graphics.GL20;

    /** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
    public class Main implements ApplicationListener {
        public static final int WORLD_WIDTH = 320;
        public static final int WORLD_HEIGHT = 180;
        private BlackjackGame game;

        @Override
        public void create() {
            game = new BlackjackGame(WORLD_WIDTH, WORLD_HEIGHT);
        }

        @Override
        public void resize(int width, int height) {
            game.resize(width, height);
        }

        @Override
        public void render() {
            float delta = Gdx.graphics.getDeltaTime();

            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            game.update(delta);
            game.render();
        }


        @Override
        public void pause() {
            // Invoked when your application is paused.
        }

        @Override
        public void resume() {
            // Invoked when your application is resumed after pause.
        }

        @Override
        public void dispose() {
            // Destroy application's resources here.
        }
    }
